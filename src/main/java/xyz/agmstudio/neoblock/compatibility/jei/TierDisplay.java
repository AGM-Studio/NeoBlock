package xyz.agmstudio.neoblock.compatibility.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import xyz.agmstudio.neoblock.data.TierData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TierDisplay {
    private final TierData data;
    private final LinkedHashMap<ItemStack, Integer> blocks = new LinkedHashMap<>();

    private final int sum;
    private final LinkedHashMap<ItemStack, Double> chances = new LinkedHashMap<>();

    public TierDisplay(TierData data) {
        int sum = 0;
        this.data = data;
        for (Map.Entry<BlockState, Integer> entry: data.blocks.entrySet()) {
            ItemStack item = new ItemStack(entry.getKey().getBlock());
            this.blocks.merge(item, entry.getValue(), Integer::sum);
            sum += entry.getValue();
        }

        this.sum = sum;
    }

    public void recalculate() {
        chances.clear();
        for (Map.Entry<ItemStack, Integer> entry: blocks.entrySet()) {
            double chance = Math.round(entry.getValue() * 10000.0 / sum) / 100.0;
            chances.put(entry.getKey(), chance);
        }
    }

    public Set<Map.Entry<ItemStack, Double>> getChances() {
        return chances.entrySet();
    }
    public double getChance(ItemStack item) {
        return chances.getOrDefault(item, 0.0);
    }
    public TierData getData() {
        return data;
    }
    public String getTierName() {
        return data.name;
    }
    public LinkedHashMap<ItemStack, Integer> getBlocks() { return blocks; }
}
