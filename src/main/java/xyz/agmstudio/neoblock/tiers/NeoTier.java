package xyz.agmstudio.neoblock.tiers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class NeoTier {
    private static final Path FOLDER = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID, "tiers");
    private static void loadFromResources(Object tier) {
        if (!Files.exists(FOLDER)) try {
            Files.createDirectories(FOLDER);
        } catch (Exception e) {
            NeoBlockMod.LOGGER.error("Unable to create folder {}", FOLDER, e);
        }

        Path location = FOLDER.resolve("tier-" + tier + ".toml");
        if (Files.exists(location)) return;

        String resource = "/configs/tiers/tier-" + tier + ".toml";
        if (!ResourceUtil.doesResourceExist(resource)) resource = "/configs/tiers/tier-template.toml";
        try {
            ResourceUtil.processResourceFile(resource, location, Map.of("[TIER]", tier.toString()));
        } catch (Exception e) {
            NeoBlockMod.LOGGER.error("Unable to process resource {}", resource, e);
        }
    }
    static {
        if (!Files.exists(FOLDER)) for (int i = 0; i < 10; i++) loadFromResources(i);
        loadFromResources("template");
    }

    public static void reload() {
        int i = 0;
        NeoBlock.TIERS.clear();
        while (Files.exists(FOLDER.resolve("tier-" + i + ".toml")))
            NeoBlock.TIERS.add(new NeoTier(i++));

        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", NeoBlock.TIERS.size());
    }

    public final CommentedFileConfig CONFIG;
    public final int TIER;
    public final int WEIGHT;
    public final int UNLOCK;

    public final List<BlockState> BLOCKS;
    public final NeoTrader UNLOCK_TRADE;

    protected NeoTier(int tier) {
        TIER = tier;
        CONFIG = CommentedFileConfig.builder(FOLDER.resolve("tier-" + TIER + ".toml")).sync().build();

        WEIGHT = CONFIG.contains("weight") ? Math.max(1, CONFIG.getInt("weight")) : 1;
        UNLOCK = CONFIG.contains("unlock") ? CONFIG.getInt("unlock") : 100 * TIER;

        List<String> blocks = CONFIG.contains("blocks") ? CONFIG.get("blocks") : List.of("minecraft:grass_block");
        BLOCKS = blocks.stream().map(String::toLowerCase)
                .map(block -> block.contains(":") ? block : "minecraft:" + block)
                .map(block -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)).defaultBlockState()).toList();

        List<String> unlockTrades = CONFIG.contains("unlock-trades") ? CONFIG.get("unlock-trades") : List.of();
        UNLOCK_TRADE = NeoTrader.parse(unlockTrades);
    }

    public int getUnlock() {
        return UNLOCK;
    }
    public int getWeight() {
        return WEIGHT;
    }
    public List<BlockState> getBlocks() {
        return BLOCKS;
    }
    public NeoTrader getUnlockTrade() {
        return UNLOCK_TRADE;
    }

    public void checkScore(Level level) {
        if (NeoBlock.BLOCK_SCOREBOARD.getScore("Total") == UNLOCK) {  // On unlock
            if (UNLOCK_TRADE != null) UNLOCK_TRADE.spawnTrader(level);
            MessagingUtil.sendMessage("messages.neoblock.unlocked_tier", level, TIER);
        }
    }
}
