package xyz.agmstudio.neoblock.data;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.world.level.block.state.BlockState;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.neo.merchants.NeoOffer;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TierData {
    public static final Path FOLDER = MinecraftUtil.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");
    public static final List<TierData> DATA = new ArrayList<>();
    private static String HASH = "";

    public static void reload() {
        ResourceUtil.loadAllTierConfigs();

        int i = 0;
        DATA.clear();
        StringBuilder hash = new StringBuilder();
        while (Files.exists(TierData.FOLDER.resolve("tier-" + i + ".toml"))) {
            TierData data = new TierData(i++);
            hash.append(HASH.isEmpty() ? "" : "|").append(data.getHashCode());
            DATA.add(data);
        }
        HASH = StringUtil.encodeToBase64(hash.toString());


        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", DATA.size());
    }

    public static TierData get(int tier) {
        try {
            return DATA.get(tier);
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }
    public static Stream<TierData> stream() {
        return DATA.stream();
    }

    public static String getHash() {
        return HASH;
    }

    public final int id;
    public final int weight;

    public final String name;
    public final TierLock lock;

    public final HashMap<BlockState, Integer> blocks = new HashMap<>();
    public final NeoMerchant tradeOffer;
    public final List<NeoOffer> trades;
    public final int tradeCount;

    protected TierData(int id) {
        this.id = id;
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "tier-" + id);
        if (config == null) throw new RuntimeException("Unable to find config for tier " + id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", id);
        name = config.getOrElse("name", "Tier-" + id);

        UnmodifiableConfig lockConfig = config.get("unlock");
        lock = id > 0 ? lockConfig != null ? new TierLock(this.id, lockConfig) : TierLock.CommandOnly(this.id) : TierLock.Unlocked();

        List<String> blocks = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks.stream().map(StringUtil::parseBlock).forEach(parsed -> this.blocks.merge(parsed.getKey().defaultBlockState(), parsed.getValue().get(), Integer::sum));
        if (this.blocks.isEmpty()) weight = 0;
        else weight = Math.max(0, config.getIntOrElse("weight", 1));

        List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        tradeOffer = NeoMerchant.parse(unlockTrades);

        List<String> trades = config.getOrElse("trader-trades", List.of());
        this.trades = trades.stream().map(NeoOffer::parse).filter(Objects::nonNull).toList();
        tradeCount = MinecraftUtil.MathUtil.clamp(config.getIntOrElse("trader-count", 0), 0, this.trades.size());

        NeoBlockMod.LOGGER.debug("Tier {} loaded. Hash key: {}", id, getHashCode());
    }

    // Coding methods to help validate world using matching config.
    // Only hash game breaking data, no need for general data like blocks, weight, etc...
    public String getHashCode() {
        String data = id + ":" + (lock == null ? "" : lock.hash());
        return StringUtil.encodeToBase64(data);
    }
}
