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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.NeoBlockBlockCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.neo.block.NeoChestSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoTagBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.NeoTagItemSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.tiers.TierConfig;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.schematics.Schematic;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.commands.NeoCommand;
import xyz.agmstudio.neocore.data.NBTSaveable;
import xyz.agmstudio.neocore.platform.IConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class WorldManager extends SavedData {
    private static final String BLOCK_BREAK_OBJECTIVE = "neoblocks_broken";

    private static final HashMap<String, HashMap<String, TierConfig>> tierConfigs = new HashMap<>();
    public static void reloadTiersConfig() {
        TierConfig.loadAllTierConfigs();
        tierConfigs.clear();

        HashMap<String, TierConfig> rootTiers = loadTiersFromDirectory(TierSpec.FOLDER);
        tierConfigs.put(null, rootTiers);
        NeoBlockMod.getLogger().info("Loaded {} tiers as default tiers.", rootTiers.size());

        try (Stream<Path> stream = Files.list(TierSpec.FOLDER)) {
            stream.filter(Files::isDirectory).forEach(folder -> {
                String folderName = folder.getFileName().toString();
                HashMap<String, TierConfig> subTiers = loadTiersFromDirectory(folder);
                tierConfigs.put(folderName, subTiers);
                NeoBlockMod.getLogger().info("Loaded {} tiers for group '{}'.", subTiers.size(), folderName);
            });
        } catch (IOException e) {
            NeoBlockMod.getLogger().error("Failed to read subdirectories from tier folder", e);
        }
    }

    private static @NotNull HashMap<String, TierConfig> loadTiersFromDirectory(@NotNull Path dir) {
        HashMap<String, TierConfig> tiers = new HashMap<>();

        if (Files.exists(dir)) try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String filename = path.getFileName().toString();
                if (!filename.endsWith(".toml") || filename.contains("template")) return;

                IConfig config = NeoBlockMod.get().getConfig(dir, filename);
                if (config != null) tiers.put(filename.substring(0, filename.length() - 5), new TierConfig(config));
                else NeoBlockMod.getLogger().warn("Failed to load tier config for file: {}", filename);
            });
        } catch (Exception e) {
            NeoBlockMod.getLogger().error("Failed to read files from directory: {}", dir, e);
        }

        return tiers;
    }
    public static HashMap<String, TierConfig> getTierConfigGroup(@Nullable String group) {
        return tierConfigs.get(group);
    }
    public static TierConfig getTierConfig(String name) {
        return getTierConfig(null, name);
    }
    public static @Nullable TierConfig getTierConfig(String group, String name) {
        HashMap<String, TierConfig> tierGroup = tierConfigs.get(group);
        return tierGroup != null ? tierGroup.get(name) : null;
    }

    private static WorldManager instance;
    public static WorldManager get() {
        return instance;
    }

    public static void reloadConfig() {
        NeoBlockMod.get().reloadModConfig();

        NeoTagItemSpec.reloadTags();
        NeoTagBlockSpec.reloadTags();
        NeoChestSpec.reloadChests();
        NeoSeqBlockSpec.reloadSequences();

        NeoTrade.reloadTrades();
        NeoMerchant.loadConfig();

        WorldManager.reloadTiersConfig();

        ForgivingVoid.loadConfig();

        Animation.reloadAnimations();
    }

    public static void setup(@NotNull ServerLevel level) {
        WorldManager.reloadConfig();
        WorldManager.instance = NeoBlockMod.captureSavedData(level, "neo_block_data", t -> WorldManager.load(t, level), () -> WorldManager.create(level));
    }

    private record ConfigPos(String id, String group, BlockPos pos) {}
    private static @NotNull List<ConfigPos> getConfigPositions(@NotNull ServerLevel level, @NotNull IConfig config) {
        List<ConfigPos> positions = new ArrayList<>();
        for (String key: config.getSection("world").sections()) {
            if (!key.equals("block") && !key.startsWith("block-")) continue;

            IConfig section = config.getSection("world." + key);
            String dimension = section.get("dimension");
            if (dimension == null) {
                if (!level.dimension().location().toString().equals("minecraft:overworld")) continue;
            } else if (!dimension.equals(level.dimension().location().toString())) continue;

            BlockPos pos = new BlockPos(section.getInt("x"), section.getInt("y"), section.getInt("z"));
            String id = key.equals("block") ? "main" : key.substring(6);
            positions.add(new ConfigPos(id, section.get("group", null), pos));
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

            for (ConfigPos pos: getConfigPositions(level, config)) {
                NeoBlock generated = NeoBlock.create(level, pos.id, pos.pos, pos.group);
                data.blocks.add(generated);
                generated.initiate();
                NeoBlockMod.getLogger().info("NeoBlock with id \"{}\" is created at {}.", generated.id, generated.pos.toShortString());
            }

            data.setDirty();
        } else {
            Optional<NeoBlockBlockCommand.AddBlock> command = NeoCommand.getFromRegistry(NeoBlockBlockCommand.AddBlock.class);

            NeoBlockMod.getLogger().info("NeoBlock has been disabled.");
            NeoBlockMod.sendMessage("message.neoblock.disabled_world_1", level, false);
            NeoBlockMod.sendMessage("message.neoblock.disabled_world_2", level, false, command.map(NeoCommand::getCommand).orElse(null));
        }

        return data;
    }
    public static @NotNull WorldManager load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldManager data = NeoBlockMod.instanceWorldData(level);

        NeoBlockMod.getLogger().debug("Loading WorldData from {}", tag);
        data.blocks.clear();
        tag.getList("Blocks", StringTag.TAG_COMPOUND).forEach(t -> {
            CompoundTag bt = (CompoundTag) t;
            if (!level.dimension().location().toString().equals(bt.getString("Dimension"))) return;
            NeoBlock block = NBTSaveable.instance(NeoBlock.class, bt, level, data);
            data.blocks.add(block);
        });

        return data;
    }

    public @NotNull CompoundTag saveDataOnTag(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (NeoBlock block: blocks) list.add(block.save());
        tag.put("Blocks", list);

        NeoBlockMod.getLogger().debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;
    private final List<NeoBlock> blocks = new ArrayList<>();

    public WorldManager(ServerLevel level) {
        this.level = level;
    }

    public static @NotNull RandomSource getRandom() {
        if (instance == null) return RandomSource.create();
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

    public TierConfig getTier(@Nullable String group, String id) {
        HashMap<String, TierConfig> map = tierConfigs.get(group);
        if (map == null) return null;
        return map.get(id);
    }
    public HashMap<String, TierConfig> getTiers(String group) {
        return tierConfigs.get(group);
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
    public static void addNeoBlock(NeoBlock block) {
        instance.blocks.add(block);
    }
    public static void removeNeoBlock(NeoBlock block) {
        instance.blocks.removeIf(b -> block.getID().equals(b.getID()));
    }
    public static boolean isNeoBlock(ServerLevel level, BlockPos pos) {
        for (NeoBlock block: instance.blocks)
            if (block.isCorrectDimension(level) && block.getBlockPos().equals(pos)) return true;

        return false;
    }
}