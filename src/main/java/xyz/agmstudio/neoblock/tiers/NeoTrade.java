package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;
import java.util.random.RandomGenerator;

public record NeoTrade(Item result, Range resultCount, Item costA, Range costACount, Item costB, Range costBCount, Range uses) {
    public static NeoTrade parse(String trade) {
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

        return new NeoTrade(resultItem, resultCount, costAItem, costACount, costBItem, costBCount, uses);
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
}
