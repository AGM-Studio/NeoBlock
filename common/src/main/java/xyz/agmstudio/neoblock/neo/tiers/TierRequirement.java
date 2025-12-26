package xyz.agmstudio.neoblock.neo.tiers;

import xyz.agmstudio.neoblock.compatibility.jei.NeoJEIPlugin;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface TierRequirement {
    void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec);
    boolean isMet(WorldManager data, TierSpec spec);
    String hash();

    class BlockBroken implements TierRequirement {
        private final long count;

        public BlockBroken(long count) {
            this.count = count;
        }

        @Override public boolean isMet(WorldManager data, TierSpec spec) {
            return data.getStatus().getBlockCount() >= count;
        }

        @Override public String hash() {
            return String.valueOf(count);
        }

        @Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
            int count = WorldManager.getWorldStatus().getBlockCount();
            NeoJEIPlugin.addBox(boxes, "jei.neoblock.requirement.blocks_broken", 7, y.getAndAdd(12), this.count <= count, this.count, count);
        }
    }

    class GameTime implements TierRequirement {
        private final long time;

        public GameTime(long time) {
            this.time = time;
        }

        @Override public boolean isMet(WorldManager data, TierSpec spec) {
            return data.getLevel().getGameTime() >= time;
        }

        @Override public String hash() {
            return String.valueOf(time);
        }

        @Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
            long time = WorldManager.getWorldLevel().getGameTime();
            NeoJEIPlugin.addBox(boxes,
                    "jei.neoblock.requirement.play_time", 7, y.getAndAdd(12), this.time <= time,
                    StringUtil.formatTicks(this.time), StringUtil.formatTicks(time)
            );
        }
    }

    class Special implements TierRequirement {
        @Override public boolean isMet(WorldManager data, TierSpec spec) {
            return spec.id == 0 || spec.commanded;
        }

        @Override public String hash() {
            return "V6Special";
        }

        @Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
            NeoJEIPlugin.addBox(boxes,
                    spec.commanded ? "jei.neoblock.requirement.command.met" : "jei.neoblock.requirement.command",
                    7, y.addAndGet(12), spec.commanded
            );
        }
    }
}
