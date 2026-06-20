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
import org.jetbrains.annotations.Unmodifiable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.NeoblockForceCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.configs.TierConfig;
import xyz.agmstudio.neoblock.neo.block.NeoChestSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoTagBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.NeoTagItemSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.schematics.Schematic;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.commands.NeoCommand;
import xyz.agmstudio.neocore.data.NBTSaveable;
import xyz.agmstudio.neocore.platform.IConfig;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class WorldManager extends SavedData {
    private static final String BLOCK_BREAK_OBJECTIVE = "neoblocks_broken";
    private static final double AABB_RANGE = 1.0;
    private final List<NeoBlock> blocks = new ArrayList<>();

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
        WorldManager.reloadConfig();
        WorldManager.instance = NeoBlockMod.captureSavedData(level, "neo_block_data", t -> WorldManager.load(t, level), () -> WorldManager.create(level));
    }

    private static @NotNull List<BlockPos> getConfigPositions(@NotNull ServerLevel level, @NotNull IConfig config) {
        List<BlockPos> positions = new ArrayList<>();
        IConfig blockSection = config.getSection("world.block");

        String blockDimension = blockSection.get("dimension");
        if (blockDimension == null || !blockDimension.equals(level.dimension().location().toString()))
            positions.add(new BlockPos(blockSection.getInt("x"), blockSection.getInt("y"), blockSection.getInt("z")));

        int blockSectionCounter = 0;
        blockSection = config.getSection("world.block-" + (++blockSectionCounter));
        while (blockSection != null) {
            blockDimension = blockSection.get("dimension");
            if (blockDimension != null && blockDimension.equals(level.dimension().location().toString())) continue;
            positions.add(new BlockPos(blockSection.getInt("x"), blockSection.getInt("y"), blockSection.getInt("z")));
            blockSection = config.getSection("world.block-" + (++blockSectionCounter));
        }

        return positions;
    }

    public static @NotNull WorldManager create(@NotNull ServerLevel level) {
        WorldManager data = NeoBlockMod.instanceWorldData(level);

        IConfig config = NeoBlockMod.getConfig();
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        if (gen instanceof FlatLevelSource || config.get("world.force-block", false)) {
            IConfig rules = config.getSection("rules");
            if (rules != null) WorldRules.applyGameRules(level, rules);

            // Load schematics from config!
            int iterator = 0;
            while (config.contains("schematics." + iterator)) {
                try {
                    IConfig scheme = config.getSection("schematics." + iterator);
                    String name = scheme.get("name", "NeoBlockSchematic_" + iterator);
                    BlockPos pos = new BlockPos(scheme.getInt("x"), scheme.getInt("y"), scheme.getInt("z"));
                    int result = Schematic.loadSchematic(level, pos, name);
                    if (result == 0) throw new FileNotFoundException("File \"" + name + "\" not found");
                } catch (Exception e) {
                    NeoBlockMod.getLogger().error("Unable to load schematic {}", iterator, e);
                }
                iterator++;
            }

            for (BlockPos pos: getConfigPositions(level, config)) {
                NeoBlock block = new NeoBlock(level, data);
                block.dimension = level.dimension().location().toString();
                block.pos = pos;
                block.initiate(level);
                data.blocks.add(block);
            }

            instance.setDirty();
        } else {
            Optional<NeoblockForceCommand.SetBlock> command = NeoCommand.getFromRegistry(NeoblockForceCommand.SetBlock.class);

            NeoBlockMod.getLogger().info("NeoBlock has been disabled.");
            NeoBlockMod.sendMessage("message.neoblock.disabled_world_1", level, false);
            NeoBlockMod.sendMessage("message.neoblock.disabled_world_2", level, false, command.map(NeoCommand::getCommand).orElse(null));
        }

        data.tiers.addAll(fetchTiers(true));

        return data;
    }
    public static @NotNull WorldManager load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldManager data = NeoBlockMod.instanceWorldData(level);

        NeoBlockMod.getLogger().debug("Loading WorldData from {}", tag);
        data.tiers.addAll(fetchTiers(false));
        data.blocks.clear();
        tag.getList("Blocks", StringTag.TAG_COMPOUND).forEach(t -> {
            CompoundTag bt = (CompoundTag) t;
            if (!level.dimension().location().toString().equals(bt.getString("dimension"))) return;
            NeoBlock block = NBTSaveable.instance(NeoBlock.class, bt, level, data);
            data.blocks.add(block);
        });

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
        ListTag list = new ListTag();
        for (NeoBlock block: blocks) list.add(block.save());
        tag.put("Blocks", list);

        NeoBlockMod.getLogger().debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;
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

    // Scoreboard manager
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

    public static void tick(ServerLevel level) {
        if (instance == null) return;
        for (NeoBlock block: instance.blocks) if (block.level == level) block.tick();
    }
    public static @NotNull @Unmodifiable List<NeoBlock> getBlocks() {
        return List.copyOf(instance.blocks);
    }
    public static boolean isNeoBlock(ServerLevel level, BlockPos pos) {
        for (NeoBlock block: instance.blocks)
            if (block.isCorrectDimension(level) && block.getBlockPos().equals(pos)) return true;

        return false;
    }
}