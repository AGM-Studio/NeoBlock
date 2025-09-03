package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.NeoblockForceCommand;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoChestSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.MessengerUtil;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class WorldData extends SavedData {
    private static final String BLOCK_BREAK_OBJECTIVE = "neoblocks_broken";

    private static WorldData load(ServerLevel level) {
        return NeoBlock.captureSavedData(level, "neo_block_data", t -> WorldData.load(t, level), () -> WorldData.create(level));
    }

    private static WorldData instance;
    public static WorldData getInstance() {
        return instance;
    }

    public static List<TierSpec> resetTiers() {
        return resetTiers(instance);
    }
    public static List<TierSpec> resetTiers(WorldData data) {
        data.tiers.clear();
        data.tiers.addAll(TierManager.fetchTiers(true));
        return data.tiers;
    }

    public static void reloadConfig() {
        NeoBlock.reloadConfig();

        NeoTrade.reloadTrades();
        NeoMerchant.loadConfig();

        NeoChestSpec.reloadChests();
        NeoSeqBlockSpec.reloadSequences();

        ForgivingVoid.loadConfig();

        Animation.reloadAnimations();
    }

    public static void setup(@NotNull ServerLevel level) {
        reloadConfig();
        instance = load(level);

        if (instance == null) return;
        IConfig config = NeoBlock.getConfig();
        if (instance.status.state == WorldStatus.State.INACTIVE) {
            boolean allowNeoBlock = true;
            final int x = config.get("world.block.x", 0);
            final int y = config.get("world.block.y", 64);
            final int z = config.get("world.block.z", 0);

            ChunkGenerator gen = level.getChunkSource().getGenerator();
            if (gen instanceof FlatLevelSource || config.get("world.force-block", false)) {
                getWorldStatus().setBlockPos(new BlockPos(x, y, z), level);
                TierSpec tier0 = getWorldTier(0);
                if (tier0 != null) tier0.getStartSequence().addToQueue(false);

                IConfig rules = config.getSection("rules");
                if (rules != null) WorldRules.applyGameRules(level, rules);

                // Load schematics from config!
                Schematic.loadSchematic(level, BlockManager.getBlockPos(), "main.nbt");
                BlockManager.updateBlock(level, false);
                int iterator = 0;
                while (config.contains("schematics.custom_" + iterator)) {
                    try {
                        IConfig scheme = config.getSection("schematics.custom_" + iterator);
                        String name = scheme.get("name", "NeoBlockSchematic_" + iterator);
                        BlockPos pos = new BlockPos(scheme.getInt("x"), scheme.getInt("y"), scheme.getInt("z"));
                        int result = Schematic.loadSchematic(level, pos, name);
                        if (result == 0) throw new FileNotFoundException("File \"" + name + "\" not found");
                    } catch (Exception e) {
                        NeoBlock.LOGGER.error("Unable to load schematic {}", iterator, e);
                    }
                    iterator++;
                }

                instance.status.state = WorldStatus.State.ACTIVE;
                instance.setDirty();
            } else {
                Optional<NeoblockForceCommand.SetBlock> command = NeoCommand.getFromRegistry(NeoblockForceCommand.SetBlock.class);

                NeoBlock.LOGGER.info("NeoBlock has been disabled.");
                MessengerUtil.sendMessage("message.neoblock.disabled_world_1", level, false);
                MessengerUtil.sendMessage("message.neoblock.disabled_world_2", level, false, command.map(NeoCommand::getCommand).orElse(null));

                instance.status.state = WorldStatus.State.DISABLED;
                instance.setDirty();
            }
        } else if (instance.status.state == WorldStatus.State.UPDATED) {
            Optional<NeoblockForceCommand.ResetTiers> command = NeoCommand.getFromRegistry(NeoblockForceCommand.ResetTiers.class);

            NeoBlock.LOGGER.info("NeoBlock tiers has been updated.");
            MessengerUtil.sendMessage("message.neoblock.updated_world", level, false, command.map(NeoCommand::getCommand).orElse(null));

            instance.status.state = WorldStatus.State.UPDATED;
            instance.setDirty();
        } else if (instance.status.state == WorldStatus.State.ACTIVE) TierManager.reloadResearches();
    }

    public static @NotNull WorldData create(@NotNull ServerLevel level) {
        WorldData data = NeoBlock.instanceWorldData(level);

        data.status = new WorldStatus(data);
        data.tiers.addAll(TierManager.fetchTiers(true));

        NeoBlock.LOGGER.debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldData load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldData data = NeoBlock.instanceWorldData(level);

        NeoBlock.LOGGER.debug("Loading WorldData from {}", tag);
        data.status = NBTSaveable.instance(WorldStatus.class, tag, data);
        data.tiers.addAll(TierManager.fetchTiers(false));

        boolean isUpdated = false;
        final ListTag tiers = tag.getList("Tiers", StringTag.TAG_COMPOUND);
        for (int i = 0; i < tiers.size(); i++) {
            CompoundTag tt = tiers.getCompound(i);
            int id = tt.getInt("id");

            if (id >= data.tiers.size()) {
                isUpdated = true;
                continue;
            }
            TierSpec tier = data.tiers.get(id);
            tier.load(tt);

            if (!tier.isStable()) {
                MessengerUtil.sendMessage("message.neoblock.tier_updated", level, false, id);
                isUpdated = true;
            }
        }

        if (isUpdated || tiers.size() < data.tiers.size()) data.status.setUpdated();
        return data;
    }

    public @NotNull CompoundTag saveDataOnTag(@NotNull CompoundTag tag) {
        tag.merge(status.save());
        ListTag list = new ListTag();
        for (TierSpec tier: tiers) list.add(tier.save());
        tag.put("Tiers", list);

        NeoBlock.LOGGER.debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;

    private WorldStatus status;
    private final List<TierSpec> tiers = new ArrayList<>();

    public WorldData(ServerLevel level) {
        this.level = level;
    }

    public static @NotNull RandomSource getRandom() {
        return instance.level.getRandom();
    }
    public WorldStatus getStatus() {
        return status;
    }
    public static WorldStatus getWorldStatus() {
        return instance.status;
    }
    public ServerLevel getLevel() {
        return level;
    }
    public static ServerLevel getWorldLevel() {
        return instance.level;
    }
    public TierSpec getTier(int id) {
        for (TierSpec tier: tiers) if (tier.getID() == id) return tier;
        return null;
    }
    public static TierSpec getWorldTier(int id) {
        return instance.getTier(id);
    }

    public List<TierSpec> getTiers() {
        return tiers;
    }
    public static List<TierSpec> getWorldTiers() {
        if (instance == null) return List.of();
        return Collections.unmodifiableList(instance.tiers);
    }
    public static int totalWeight() {
        return instance.tiers.stream().filter(TierSpec::isEnabled).mapToInt(TierSpec::getWeight).sum();
    }

    public static void setCommanded(TierSpec tier, boolean force) {
        tier.setSpecialRequirement(true);

        if (force && tier.canBeResearched()) tier.startResearch();
    }

    private static Objective getObjective(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(BLOCK_BREAK_OBJECTIVE);
        if (objective != null) return objective;

        objective = MinecraftUtil.createScoreboardObjective(scoreboard, BLOCK_BREAK_OBJECTIVE, ObjectiveCriteria.DUMMY, "scoreboard.neoblock.title", ObjectiveCriteria.RenderType.INTEGER);
        MinecraftUtil.setScoreboardDisplay(scoreboard, MinecraftUtil.ScoreboardSlots.LIST, objective);
        MinecraftUtil.setScoreboardDisplay(scoreboard, MinecraftUtil.ScoreboardSlots.BELOW_NAME, objective);

        return objective;
    }
    public static void addBlocksBroken(ServerPlayer player, int amount) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        MinecraftUtil.addPlayerScore(scoreboard, player, objective, amount);
    }
    public static void setBlocksBroken(ServerPlayer player, int amount) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        MinecraftUtil.setPlayerScore(scoreboard, player, objective, amount);
    }
    public static int getBlocksBroken(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        return MinecraftUtil.getPlayerScore(scoreboard, player, objective);
    }
}
