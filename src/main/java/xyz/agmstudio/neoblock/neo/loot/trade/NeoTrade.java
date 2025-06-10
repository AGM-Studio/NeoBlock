package xyz.agmstudio.neoblock.neo.loot.trade;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.*;

public abstract class NeoTrade {
    private static final Path FOLDER = MinecraftUtil.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID);
    private static final HashMap<String, List<NeoTrade>> TRADES = new HashMap<>();
    public static List<NeoTrade> getTrades(String name) {
        return TRADES.getOrDefault(name, List.of());
    }

    public static void reloadTrades() {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "trades");
        if (config == null) {
            NeoBlockMod.LOGGER.error("Failed to load trades config.");
            return;
        }

        TRADES.clear();

        for (String key : config.valueMap().keySet()) {
            Set<String> visited = new HashSet<>();
            List<String> entries = config.get(key);
            if (entries == null) {
                TRADES.put(key, List.of());
                NeoBlockMod.LOGGER.info("Loaded 0 trades for {}", key);
            }
            else {
                List<NeoTrade> list = new ArrayList<>();
                for (String entry : entries) {
                    NeoTradeGroup.parse(entry).ifPresent(list::add);
                    NeoTradeSingle.parse(entry).ifPresent(list::add);
                }
                TRADES.put(key, list);
                NeoBlockMod.LOGGER.info("Loaded {} trades for {}", list.size(), key);
            }
        }

        NeoBlockMod.LOGGER.info("Loaded {} trades.", TRADES.size());
    }

    public abstract Optional<MerchantOffer> getOffer();
}
