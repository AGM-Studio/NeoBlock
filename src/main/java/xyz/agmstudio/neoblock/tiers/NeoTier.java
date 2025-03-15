package xyz.agmstudio.neoblock.tiers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.random.RandomGenerator;

public class NeoTier {
    protected static final Path FOLDER = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID, "tiers");
    protected static void loadFromResources(Object tier) {
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
            ResourceUtil.processResourceFile(resource, location, Map.of("[TIER]", tier == "template" ? "10" : tier.toString()));
        } catch (Exception e) {
            NeoBlockMod.LOGGER.error("Unable to process resource {}", resource, e);
        }
    }

    public final CommentedFileConfig CONFIG;
    public final int TIER;
    public final int WEIGHT;
    public final int UNLOCK;
    public final int UNLOCK_TIME;

    public final HashMap<BlockState, Integer> BLOCKS = new HashMap<>();
    public final NeoMerchant UNLOCK_TRADE;
    public final List<NeoOffer> TRADES;
    public final int TRADE_COUNT;

    public @Nullable NeoTier previous() {
        return TIER > 0 ? NeoBlock.TIERS.get(this.TIER - 1) : null;
    }
    public @Nullable NeoTier next() {
        return TIER < NeoBlock.TIERS.size() - 1 ? NeoBlock.TIERS.get(this.TIER + 1) : null;
    }
    public List<NeoTier> allPrevious() {
        return NeoBlock.TIERS.subList(0, this.TIER + 1);
    }
    public List<NeoTier> allNext() {
        return NeoBlock.TIERS.subList(this.TIER + 1, NeoBlock.TIERS.size());
    }

    protected NeoTier(int tier) {
        TIER = tier;
        CONFIG = ResourceUtil.getConfig(FOLDER, "tier-" + tier);
        if (CONFIG == null) throw new RuntimeException("Unable to find config for tier " + tier);

        NeoTier previous = previous();
        WEIGHT = Math.max(1, CONFIG.getIntOrElse("weight", 1));
        UNLOCK = Math.max(
                previous == null ? 0 : previous.getUnlock() + 1,
                CONFIG.getIntOrElse("unlock", 100 * TIER)
        );
        UNLOCK_TIME = Math.max(0, CONFIG.getIntOrElse("unlock-time", 0));

        List<String> blocks = CONFIG.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks.stream().map(StringUtil::parseBlock).forEach(parsed -> BLOCKS.merge(parsed.getKey().defaultBlockState(), parsed.getValue().get(), Integer::sum));
        if (BLOCKS.isEmpty()) {
            NeoBlockMod.LOGGER.error("No blocks found for tier {}", TIER);
            BLOCKS.put(NeoBlock.DEFAULT_STATE, 1);
        }

        List<String> unlockTrades = CONFIG.getOrElse("unlock-trades", List.of());
        UNLOCK_TRADE = NeoMerchant.parse(unlockTrades);

        List<String> trades = CONFIG.getOrElse("trader-trades", List.of());
        TRADES = trades.stream().map(NeoOffer::parse).toList();
        TRADE_COUNT = Math.clamp(CONFIG.getIntOrElse("trader-count", 0), 0, TRADES.size());
    }

    public List<NeoOffer> getRandomTrades() {
        List<NeoOffer> trades = new ArrayList<>(TRADES);
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

    public void onGettingUnlocked(ServerLevel level) {
        MessagingUtil.sendInstantMessage("message.neoblock.unlocked_tier", level, false, TIER);
        if (UNLOCK_TRADE != null && level instanceof ServerLevel server) {
            UNLOCK_TRADE.spawnTrader(server, "UnlockTrader");
            MessagingUtil.sendInstantMessage("message.neoblock.unlocked_trader", level, false, TIER);
        }
    }

    public boolean isUnlocked() {
        return NeoBlock.DATA != null && NeoBlock.DATA.getBlockCount() >= UNLOCK;
    }
}
