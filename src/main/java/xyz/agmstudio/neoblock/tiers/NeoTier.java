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
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NeoTier {
    protected static final Path FOLDER = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID, "tiers");
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

    public final CommentedFileConfig CONFIG;
    public final int TIER;
    public final int WEIGHT;
    public final int UNLOCK;

    public final HashMap<BlockState, Integer> BLOCKS;
    public final NeoTrader UNLOCK_TRADE;
    public final List<NeoTrade> TRADES;
    public final int TRADE_COUNT;

    protected NeoTier(int tier) {
        TIER = tier;
        CONFIG = CommentedFileConfig.builder(FOLDER.resolve("tier-" + TIER + ".toml")).sync().build();
        CONFIG.load();

        WEIGHT = CONFIG.contains("weight") ? Math.max(1, CONFIG.getInt("weight")) : 1;
        UNLOCK = CONFIG.contains("unlock") ? CONFIG.getInt("unlock") : 100 * TIER;

        List<String> blocks = CONFIG.contains("blocks") ? CONFIG.get("blocks") : List.of("minecraft:grass_block");
        BLOCKS = new HashMap<>();
        Pattern pattern = Pattern.compile("^(\\d+)x");
        for (String block: blocks) {
            Matcher matcher = pattern.matcher(block);
            int count = 1;
            if (matcher.find()) {
                count = Integer.parseInt(matcher.group(1));
                block = block.substring(matcher.end());
            }
            try {
                BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)).defaultBlockState();
                NeoBlockMod.LOGGER.debug("Reading '{}' resulted in block {} x{}", block, state, count);
                BLOCKS.merge(state, count, Integer::sum);
            } catch (Exception e) {
                NeoBlockMod.LOGGER.error("Failed to parse block '{}': {}", block, e.getMessage());
            }
        }
        if (BLOCKS.isEmpty()) NeoBlockMod.LOGGER.error("No blocks found for tier {}", TIER);
        NeoBlockMod.LOGGER.debug("Loaded {} blocks from the tier {}\n{}", BLOCKS.size(), TIER, BLOCKS.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(",\n\t", "{\n\t", "\n}")));

        List<String> unlockTrades = CONFIG.contains("unlock-trades") ? CONFIG.get("unlock-trades") : List.of();
        UNLOCK_TRADE = NeoTrader.parse(unlockTrades);

        List<String> trades = CONFIG.contains("wandering-trades") ? CONFIG.get("wandering-trades") : List.of();
        TRADES = trades.stream().map(NeoTrade::parse).toList();

        int tradeCount = CONFIG.contains("wandering-count") ? CONFIG.getInt("wandering-count") : 0;
        TRADE_COUNT = Math.clamp(tradeCount, 0, TRADES.size());
    }

    public List<NeoTrade> getRandomTrades() {
        List<NeoTrade> trades = new ArrayList<>(TRADES);
        Collections.shuffle(trades);
        return trades.subList(0, TRADE_COUNT);
    }
    public BlockState getRandomBlock() {
        if (BLOCKS.isEmpty()) return NeoBlock.DEFAULT_STATE;

        int totalWeight = BLOCKS.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = RandomGenerator.getDefault().nextInt(totalWeight);
        for (Map.Entry<BlockState, Integer> entry: BLOCKS.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue < 0) return entry.getKey();
        }

        NeoBlockMod.LOGGER.error("Unable to get a random block from tier {}", TIER);
        return BLOCKS.keySet().stream().findFirst().orElse(NeoBlock.DEFAULT_STATE);
    }

    public int getUnlock() {
        return UNLOCK;
    }
    public int getWeight() {
        return WEIGHT;
    }

    public void checkScore(Level level) {
        if (NeoBlock.DATA.getBlockCount() == UNLOCK) {  // On unlock
            if (UNLOCK_TRADE != null) UNLOCK_TRADE.spawnTrader(level);
            MessagingUtil.sendMessage("messages.neoblock.unlocked_tier", level, TIER);
        }
    }
}
