package xyz.agmstudio.neoblock.compatibility.jei;

import net.minecraft.world.item.Item;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoChestSpec;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static xyz.agmstudio.neoblock.compatibility.jei.NeoJEIPlugin.GRAY_COLOR;
import static xyz.agmstudio.neoblock.compatibility.jei.NeoJEIPlugin.RED_COLOR;

public class TierDisplay {
    private final TierSpec tier;
    private final LinkedHashMap<Item, Integer> blocks = new LinkedHashMap<>();
    private final LinkedHashMap<Item, Double> chances = new LinkedHashMap<>();

    public TierDisplay(TierSpec tier) {
        this.tier = tier;
        LinkedHashMap<Item, Integer> blocks = new LinkedHashMap<>();
        for (NeoBlockSpec entry: tier.getBlocks()) {
            if (entry instanceof NeoChestSpec) continue;  // Todo: Support for chest loots and trades
            blocks.merge(entry.getBlock().asItem(), entry.getWeight(), Integer::sum);
        }
        blocks.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry<Item, Integer>::getValue).reversed())
                .forEachOrdered(e -> this.blocks.put(e.getKey(), e.getValue()));

        for (Map.Entry<Item, Integer> entry: this.blocks.entrySet())
            chances.put(entry.getKey(), entry.getValue().doubleValue() / tier.getTotalBlockWeight());
    }

    public Set<Map.Entry<Item, Double>> getChances() {
        return chances.entrySet();
    }
    public double getChance(Item item) {
        return chances.getOrDefault(item, 0.0);
    }
    public TierSpec getTierSpec() {
        return tier;
    }
    public String getTierName() {
        return tier.getName();
    }
    public LinkedHashMap<Item, Integer> getBlocks() { return blocks; }

    public int getTextHeight() {
        if (tier.isResearched()) return 28;
        return 40 + tier.getRequirements().size() * 12;
    }
    public List<NeoJEIPlugin.TextBox> getTextBoxes() {
        final List<NeoJEIPlugin.TextBox> boxes = new ArrayList<>();
        final AtomicInteger y = new AtomicInteger(2);

        NeoJEIPlugin.addBox(boxes,
                "jei.neoblock.tier", 2, y.getAndAdd(12),
                tier.isEnabled() && WorldManager.getWorldStatus().isActive(),
                tier.getID(), getTierName()
        );
        if (!WorldManager.getWorldStatus().isActive()) {
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.dormant", 2, y.getAndAdd(12), RED_COLOR
            );
        } else if (!tier.isResearched()) {
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.unlock_time", 2, y.getAndAdd(12), GRAY_COLOR,
                    StringUtil.formatTicks(tier.getResearchTime())
            );
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.requirements.header", 2, y.getAndAdd(12), GRAY_COLOR
            );

            tier.getRequirements().forEach(requirement -> requirement.addJEIBox(boxes, y, tier));
        } else {
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.chance", 2, y.getAndAdd(12), GRAY_COLOR,
                    StringUtil.percentage((double) tier.getWeight() / WorldManager.totalWeight(), 2)
            );
        }

        return boxes;
    }
}
