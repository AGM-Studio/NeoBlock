package xyz.agmstudio.neoblock.neo.tiers;

import xyz.agmstudio.neoblock.neo.world.WorldData;

public interface TierRequirement {
    boolean isMet(WorldData data, TierSpec spec);
    String hash();

    class BlockBroken implements TierRequirement {
        private final long count;

        public BlockBroken(long count) {
            this.count = count;
        }

        @Override public boolean isMet(WorldData data, TierSpec spec) {
            return data.getStatus().getBlockCount() >= count;
        }

        @Override public String hash() {
            return String.valueOf(count);
        }
    }

    class GameTime implements TierRequirement {
        private final long time;

        public GameTime(long time) {
            this.time = time;
        }

        @Override public boolean isMet(WorldData data, TierSpec spec) {
            return data.getLevel().getGameTime() >= time;
        }

        @Override public String hash() {
            return String.valueOf(time);
        }
    }

    class Special implements TierRequirement {
        @Override public boolean isMet(WorldData data, TierSpec spec) {
            return spec.id == 0 || spec.commanded;
        }

        @Override public String hash() {
            return "V6Special";
        }
    }
}
