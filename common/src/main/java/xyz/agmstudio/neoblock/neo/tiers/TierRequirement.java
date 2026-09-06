package xyz.agmstudio.neoblock.neo.tiers;

//import xyz.agmstudio.neoblock.compatibility.jei.NeoJEIPlugin;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
public interface TierRequirement {
    //void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec);
    boolean isMet(TierSpec spec);

    class BlockBroken implements TierRequirement {
        private final long count;

        public BlockBroken(long count) {
            this.count = count;
        }

        @Override public boolean isMet(TierSpec spec) {
            return spec.block.getBlockCount() >= count;
        }

        //@Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
        //    int count = spec.block.getBlockCount();
        //    NeoJEIPlugin.addBox(boxes, "jei.neoblock.requirement.blocks_broken", 7, y.getAndAdd(12), this.count <= count, this.count, count);
        //}
    }

    class GameTime implements TierRequirement {
        private final long time;

        public GameTime(long time) {
            this.time = time;
        }

        @Override public boolean isMet(TierSpec spec) {
            return spec.block.level.getGameTime() >= time;
        }


        //@Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
        //    long time = WorldManager.getWorldLevel().getGameTime();
        //    NeoJEIPlugin.addBox(boxes,
        //            "jei.neoblock.requirement.play_time", 7, y.getAndAdd(12), this.time <= time,
        //            StringUtil.formatTicks(this.time), StringUtil.formatTicks(time)
        //    );
        //}
    }

    class Special implements TierRequirement {
        @Override public boolean isMet(TierSpec spec) {
            return spec.commanded;
        }

        //@Override public void addJEIBox(List<NeoJEIPlugin.TextBox> boxes, AtomicInteger y, TierSpec spec) {
        //    NeoJEIPlugin.addBox(boxes,
        //            spec.commanded ? "jei.neoblock.requirement.command.met" : "jei.neoblock.requirement.command",
        //            7, y.addAndGet(12), spec.commanded
        //    );
        //}
    }
}