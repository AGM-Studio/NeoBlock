package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NeoTrade {
    protected final List<Trade> trades;

    public static NeoTrade parse(List<String> trades) {
        if (trades.isEmpty()) return null;

        NeoTrade trade = new NeoTrade();
        trades.stream().map(Trade::parse).forEach(trade.trades::add);

        return trade;
    }

    protected NeoTrade() {
        this.trades = new ArrayList<>();
    }

    public void spawnTrader(Level level) {
        Villager trader = new Villager(EntityType.VILLAGER, level);
        trader.setPos(NeoBlock.POS.getCenter().add(0, 2, 0));
        trader.setVillagerData(trader.getVillagerData()
                .setProfession(VillagerProfession.NITWIT)
                .setLevel(5)
        );

        MerchantOffers offers = new MerchantOffers();
        this.trades.stream().map(Trade::getOffer).forEach(offers::add);
        trader.setOffers(offers);

        level.addFreshEntity(trader);
    }

    protected record Range(int min, int max) {
        public static Range parse(String value) {
            String[] values = value.split("-");
            int min = Integer.parseInt(values[0]);
            int max = values.length > 1 ? Integer.parseInt(values[1]) : min;
            return new Range(min, max);
        }

        public Range(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        public int get() {
            return RandomGenerator.getDefault().nextInt(min, max + 1);
        }

        public String dump() {
            if (min == max) return min + "x";
            return min + "-" + max + "x";
        }
    }

    protected record Trade(Item result, Range resultCount, Item costA, Range costACount, Item costB, Range costBCount, Range uses) {
        public static Trade parse(String trade) {
            String[] parts = trade.split("; ");
            if (parts.length < 2) return null;

            String[] resultData = parts[0].split("x", 2);
            Item resultItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultData[1]));
            Range resultCount = Range.parse(resultData[0]);

            String[] costAData = parts[1].split("x", 2);
            Item costAItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(costAData[1]));
            Range costACount = Range.parse(costAData[0]);

            Item costBItem = null;
            Range costBCount = new Range(0, 0);
            Range uses = new Range(1, 1);
            if (parts.length > 2) {
                if (parts[2].contains("x")) {
                    String[] costBData = parts[2].split("x", 2);
                    if (costBData.length > 1) {
                        costBCount = Range.parse(costBData[0]);
                        costBItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(costBData[1]));
                    }
                } else uses = Range.parse(parts[2]);
            }

            if (parts.length > 3) uses = Range.parse(parts[3]);

            return new Trade(resultItem, resultCount, costAItem, costACount, costBItem, costBCount, uses);
        }

        public String dump() {
            String codec = resultCount.dump() + BuiltInRegistries.ITEM.getKey(result) + ";" +
                    " " + costACount.dump() + BuiltInRegistries.ITEM.getKey(costA) + ";";
            if (costB == null || costBCount == null) return codec;
            return codec + " " + costBCount.dump() + BuiltInRegistries.ITEM.getKey(costB) + ";";
        }

        public MerchantOffer getOffer() {
            return new MerchantOffer(
                    new ItemCost(costA, costACount.get()),
                    costB == null ? Optional.empty() : Optional.of(new ItemCost(costB, costBCount.get())),
                    new ItemStack(result, resultCount.get()),
                    uses.get(), 0, 0
            );
        }
    }
}
