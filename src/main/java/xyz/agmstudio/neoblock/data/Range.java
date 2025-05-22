package xyz.agmstudio.neoblock.data;

import xyz.agmstudio.neoblock.neo.world.WorldData;

public final class Range {
    final int min, max;

    public Range(int solid) {
        this.min = solid;
        this.max = solid;
    }

    public Range(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public int get() {
        if (min == max) return min;
        return WorldData.getRandom().nextInt(min, max + 1);
    }

    public String toString() {
        if (min == max) return min + "x";
        return min + "-" + max + "x";
    }
}
