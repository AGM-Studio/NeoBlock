package xyz.agmstudio.neoblock.compatibility.jei;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.data.TierLock;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoChestSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static xyz.agmstudio.neoblock.compatibility.jei.NeoJEIPlugin.GRAY_COLOR;

public class TierDisplay {
    private final TierData data;
    private final LinkedHashMap<Item, Integer> blocks = new LinkedHashMap<>();
    private final LinkedHashMap<Item, Double> chances = new LinkedHashMap<>();

    public TierDisplay(TierData data) {
        int sum = 0;
        this.data = data;
        LinkedHashMap<Item, Integer> blocks = new LinkedHashMap<>();
        for (NeoBlockSpec entry: data.blocks) {
            sum += entry.getWeight();
            if (entry instanceof NeoChestSpec) continue;  // Todo: Support for chest loots and trades
            blocks.merge(entry.getBlock().asItem(), entry.getWeight(), Integer::sum);
        }
        blocks.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry<Item, Integer>::getValue).reversed())
                .forEachOrdered(e -> this.blocks.put(e.getKey(), e.getValue()));

        for (Map.Entry<Item, Integer> entry: this.blocks.entrySet())
            chances.put(entry.getKey(), entry.getValue().doubleValue() / sum);
    }

    public Set<Map.Entry<Item, Double>> getChances() {
        return chances.entrySet();
    }
    public double getChance(Item item) {
        return chances.getOrDefault(item, 0.0);
    }
    public TierData getData() {
        return data;
    }
    public String getTierName() {
        return data.name;
    }
    public WorldTier getWorldTier() {
        return WorldData.getInstance().getTier(data.id);
    }
    public LinkedHashMap<Item, Integer> getBlocks() { return blocks; }

    public int getTextHeight() {
        WorldTier tier = getWorldTier();
        int base = 28;
        if (tier.isUnlocked()) return base;
        TierLock lock = tier.getLock();
        if (lock.getBlocks() > 0) base += 12;
        if (lock.getGameplay() > 0) base += 12;
        if (lock.isCommanded()) base += 12;
        return base + 12;
    }
    public List<NeoJEIPlugin.TextBox> getTextBoxes() {
        List<NeoJEIPlugin.TextBox> boxes = new ArrayList<>();
        WorldTier tier = getWorldTier();

        AtomicInteger y = new AtomicInteger(2);
        NeoJEIPlugin.addBox(boxes,
                "jei.neoblock.tier", 2, y.getAndAdd(12), tier.isEnabled(),
                tier.getID(), getTierName()
        );
        if (!tier.isUnlocked()) {
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.unlock_time", 2, y.getAndAdd(12), GRAY_COLOR,
                    StringUtil.formatTicks(tier.getLock().getTime())
            );
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.requirements.header", 2, y.getAndAdd(12), GRAY_COLOR
            );

            @NotNull TierLock lock = tier.getLock();
            if (lock.getBlocks() > 0) {
                int count = WorldData.getWorldStatus().getBlockCount();
                NeoJEIPlugin.addBox(boxes, "jei.neoblock.requirement.blocks_broken", 7, y.getAndAdd(12), lock.getBlocks() <= count, lock.getBlocks(), count);
            }
            if (lock.getGameplay() > 0) {
                long time = WorldData.getWorldLevel().getGameTime();
                NeoJEIPlugin.addBox(boxes,
                        "jei.neoblock.requirement.play_time", 7, y.getAndAdd(12), lock.getGameplay() <= time,
                        StringUtil.formatTicks(lock.getGameplay()), StringUtil.formatTicks(time)
                );
            }
            if (lock.isCommanded())
                NeoJEIPlugin.addBox(boxes,
                        tier.isCommanded() ? "jei.neoblock.status.requirement.command.met" : "jei.neoblock.status.requirement.command",
                        7, y.addAndGet(12), tier.isCommanded()
                );
        } else {
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.chance", 2, y.getAndAdd(12), GRAY_COLOR,
                    StringUtil.percentage((double) data.weight / WorldData.totalWeight(), 2)
            );
        }

        return boxes;
    }
}
