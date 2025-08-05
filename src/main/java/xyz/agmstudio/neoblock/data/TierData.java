package xyz.agmstudio.neoblock.data;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.loot.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TierData {
    public static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");
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

    public final List<NeoBlockSpec> blocks = new ArrayList<>();
    public final NeoTradePool tradePoolUnlock;
    public final NeoTradePool trades;

    protected TierData(int id) {
        this.id = id;
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "tier-" + id);
        if (config == null) throw new RuntimeException("Unable to find config for tier " + id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", id);
        name = config.getOrElse("name", "Tier-" + id);

        UnmodifiableConfig lockConfig = config.get("unlock");
        lock = id > 0 ? lockConfig != null ? new TierLock(this.id, lockConfig) : TierLock.CommandOnly(this.id) : TierLock.Unlocked();

        List<String> blocks = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks.forEach(value -> NeoBlockSpec.parse(value).ifPresent(this.blocks::add));

        if (this.blocks.isEmpty()) weight = 0;
        else weight = Math.max(0, config.getIntOrElse("weight", 1));

        List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        tradePoolUnlock = NeoTradePool.parse(unlockTrades);

        List<String> list = config.getOrElse("trader-trades", config.getOrElse("trades", List.of()));
        trades = NeoTradePool.parse(list);

        NeoBlockMod.LOGGER.debug("Tier {} loaded. Hash key: {}", id, getHashCode());
    }

    public static int size() {
        return DATA.size();
    }

    // Coding methods to help validate world using matching config.
    // Only hash game breaking data, no need for general data like blocks, weight, etc...
    public String getHashCode() {
        String data = id + ":" + (lock == null ? "" : lock.hash());
        return StringUtil.encodeToBase64(data);
    }

    public WorldTier getWorldTier() {
        return WorldData.getInstance().getTier(id);
    }
}
