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
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.NeoblockForceCommand;
import xyz.agmstudio.neoblock.configs.TierConfig;
import xyz.agmstudio.neocore.commands.NeoCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neocore.data.NBTSaveable;
import xyz.agmstudio.neoblock.schematics.Schematic;
import xyz.agmstudio.neoblock.neo.block.*;
import xyz.agmstudio.neoblock.neo.loot.NeoTagItemSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neocore.platform.IConfig;
import xyz.agmstudio.neocore.NeoMC;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.*;

public abstract class WorldManager extends SavedData {
    private static final String BLOCK_BREAK_OBJECTIVE = "neoblocks_broken";

    private static WorldManager load(ServerLevel level) {
        return NeoBlockMod.captureSavedData(level, "neo_block_data", t -> WorldManager.load(t, level), () -> WorldManager.create(level));
    }

    private static WorldManager instance;
    public static WorldManager getInstance() {
        return instance;
    }

    public static List<TierSpec> resetTiers() {
        return resetTiers(instance);
    }
    public static List<TierSpec> resetTiers(WorldManager data) {
        data.tiers.clear();
        data.tiers.addAll(fetchTiers(true));
        return data.tiers;
    }

    public static void reloadConfig() {
        NeoBlockMod.get().reloadModConfig();

        NeoTagItemSpec.reloadTags();
        NeoTrade.reloadTrades();
        NeoMerchant.loadConfig();

        NeoTagBlockSpec.reloadTags();
        NeoChestSpec.reloadChests();
        NeoSeqBlockSpec.reloadSequences();

        ForgivingVoid.loadConfig();

        Animation.reloadAnimations();
    }

    public static void setup(@NotNull ServerLevel level) {
        reloadConfig();
        instance = load(level);

        if (instance == null) return;
        IConfig config = NeoBlockMod.getConfig();
        if (instance.status.state == WorldData.State.INACTIVE) {
            boolean allowNeoBlock = true;
            final int x = config.get("world.block.x", 0);
            final int y = config.get("world.block.y", 64);
            final int z = config.get("world.block.z", 0);

            ChunkGenerator gen = level.getChunkSource().getGenerator();
            if (gen instanceof FlatLevelSource || config.get("world.force-block", false)) {
                getWorldData().setBlockPos(new BlockPos(x, y, z), level);
                TierSpec tier0 = getWorldTier(0);
                if (tier0 != null) tier0.getStartSequence().addToQueue(false);

                IConfig rules = config.getSection("rules");
                if (rules != null) WorldRules.applyGameRules(level, rules);

                // Load schematics from config!
                Schematic.loadSchematic(level, NeoBlockPos.get(), "main.nbt");
                int iterator = 0;
                while (config.contains("schematics.custom_" + iterator)) {
                    try {
                        IConfig scheme = config.getSection("schematics.custom_" + iterator);
                        String name = scheme.get("name", "NeoBlockSchematic_" + iterator);
                        BlockPos pos = new BlockPos(scheme.getInt("x"), scheme.getInt("y"), scheme.getInt("z"));
                        int result = Schematic.loadSchematic(level, pos, name);
                        if (result == 0) throw new FileNotFoundException("File \"" + name + "\" not found");
                    } catch (Exception e) {
                        NeoBlockMod.getLogger().error("Unable to load schematic {}", iterator, e);
                    }
                    iterator++;
                }
                instance.status.state = WorldData.State.ACTIVE;
                instance.setDirty();

                BlockManager.updateBlock(level, false);
            } else {
                Optional<NeoblockForceCommand.SetBlock> command = NeoCommand.getFromRegistry(NeoblockForceCommand.SetBlock.class);

                NeoBlockMod.getLogger().info("NeoBlock has been disabled.");
                NeoBlockMod.sendMessage("message.neoblock.disabled_world_1", level, false);
                NeoBlockMod.sendMessage("message.neoblock.disabled_world_2", level, false, command.map(NeoCommand::getCommand).orElse(null));

                instance.status.state = WorldData.State.DISABLED;
                instance.setDirty();
            }
        } else if (instance.status.state == WorldData.State.UPDATED) {
            Optional<NeoblockForceCommand.ResetTiers> command = NeoCommand.getFromRegistry(NeoblockForceCommand.ResetTiers.class);

            NeoBlockMod.getLogger().info("NeoBlock tiers has been updated.");
            NeoBlockMod.sendMessage("message.neoblock.updated_world", level, false, command.map(NeoCommand::getCommand).orElse(null));

            instance.status.state = WorldData.State.UPDATED;
            instance.setDirty();
        }
    }

    public static @NotNull WorldManager create(@NotNull ServerLevel level) {
        WorldManager data = NeoBlockMod.instanceWorldData(level);

        data.status = new WorldData(data);
        data.tiers.addAll(fetchTiers(true));

        NeoBlockMod.getLogger().debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldManager load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldManager data = NeoBlockMod.instanceWorldData(level);

        NeoBlockMod.getLogger().debug("Loading WorldData from {}", tag);
        data.status = NBTSaveable.instance(WorldData.class, tag, data);
        data.tiers.addAll(fetchTiers(false));

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
                NeoBlockMod.sendMessage("message.neoblock.tier_updated", level, false, id);
                isUpdated = true;
            }
        }

        if (isUpdated || tiers.size() < data.tiers.size()) data.status.setUpdated();
        return data;
    }

    public static List<TierSpec> fetchTiers(boolean loadConfig) {
        TierConfig.loadAllTierConfigs();

        List<TierSpec> tiers = new ArrayList<>();
        for (int i = 0; Files.exists(TierSpec.FOLDER.resolve("tier-" + i + ".toml")); i++)
            tiers.add(new TierSpec(i, loadConfig));

        NeoBlockMod.getLogger().info("Loaded {} tiers from the tiers folder.", tiers.size());
        return tiers;
    }

    public @NotNull CompoundTag saveDataOnTag(@NotNull CompoundTag tag) {
        tag.merge(status.save());
        ListTag list = new ListTag();
        for (TierSpec tier: tiers) list.add(tier.save());
        tag.put("Tiers", list);

        NeoBlockMod.getLogger().debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;

    private WorldData status;
    private final List<TierSpec> tiers = new ArrayList<>();

    public WorldManager(ServerLevel level) {
        this.level = level;
    }

    public static @NotNull RandomSource getRandom() {
        return instance.level.getRandom();
    }
    public static <T> Optional<T> getRandomItem(List<T> collection) {
        if (collection.isEmpty()) return Optional.empty();
        return Optional.of(collection.get(getRandom().nextInt(collection.size())));
    }

    public WorldData getStatus() {
        return status;
    }
    public static WorldData getWorldData() {
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

        objective = NeoMC.createScoreboardObjective(scoreboard, BLOCK_BREAK_OBJECTIVE, ObjectiveCriteria.DUMMY, "scoreboard.neoblock.title", ObjectiveCriteria.RenderType.INTEGER);
        NeoMC.setScoreboardDisplay(scoreboard, NeoMC.ScoreboardSlots.LIST, objective);
        NeoMC.setScoreboardDisplay(scoreboard, NeoMC.ScoreboardSlots.BELOW_NAME, objective);

        return objective;
    }
    public static void addBlocksBroken(ServerPlayer player, int amount) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        NeoMC.addPlayerScore(scoreboard, player, objective, amount);
    }
    public static void setBlocksBroken(ServerPlayer player, int amount) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        NeoMC.setPlayerScore(scoreboard, player, objective, amount);
    }
    public static int getBlocksBroken(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = getObjective(scoreboard);

        return NeoMC.getPlayerScore(scoreboard, player, objective);
    }
}