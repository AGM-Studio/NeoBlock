package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.ConfigUtil;

import java.util.*;

public abstract class NeoTrade {
    private static final HashMap<String, List<NeoTrade>> TRADES = new HashMap<>();
    public static List<NeoTrade> getTrades(String name) {
        return TRADES.getOrDefault(name, List.of());
    }

    public static void reloadTrades() {
        IConfig config = ConfigUtil.getConfig(NeoBlock.CONFIG_FOLDER, "trades");
        if (config == null) {
            NeoBlock.LOGGER.error("Failed to load trades config.");
            return;
        }

        TRADES.clear();

        for (String key : config.valueMap().keySet()) {
            List<String> entries = config.get(key);
            if (entries == null) {
                TRADES.put(key, List.of());
                NeoBlock.LOGGER.info("Loaded 0 trades for {}", key);
            }
            else {
                List<NeoTrade> list = new ArrayList<>();
                for (String entry : entries) {
                    NeoTradeGroup.parse(entry).ifPresent(list::add);
                    NeoTradeSingle.parse(entry).ifPresent(list::add);
                }
                TRADES.put(key, list);
                NeoBlock.LOGGER.info("Loaded {} trades for {}", list.size(), key);
            }
        }

        NeoBlock.LOGGER.info("Loaded {} trades.", TRADES.size());
    }

    public abstract Optional<MerchantOffer> getOffer();
}
