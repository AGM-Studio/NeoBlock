package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NeoTradePool {
    private final List<NeoTrade> trades;

    public NeoTradePool(List<NeoTrade> trades) {
        this.trades = new ArrayList<>(trades);
    }

    public static NeoTradePool parse(List<String> entries) {
        List<NeoTrade> list = new ArrayList<>();
        for (String entry : entries) {
            Optional<NeoTrade> group = NeoTradeGroup.parse(entry);
            if (group.isPresent()) list.add(group.get());
            else NeoTradeSingle.parse(entry).ifPresent(list::add);
        }

        return new NeoTradePool(list);
    }

    public List<MerchantOffer> getAllOffers() {
        List<MerchantOffer> offers = new ArrayList<>();
        for (NeoTrade trade :trades) trade.getOffer().ifPresent(offers::add);
        return offers;
    }

    public int size() {
        return trades.size();
    }

    public void addTrade(NeoTrade trade) {
        trades.add(trade);
    }

    public void clear() {
        trades.clear();
    }

    public List<NeoTrade> getPool() {
        return Collections.unmodifiableList(trades);
    }
}
