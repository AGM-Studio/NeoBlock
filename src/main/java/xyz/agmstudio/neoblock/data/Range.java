package xyz.agmstudio.neoblock.data;

import java.util.random.RandomGenerator;

public final class Range {
    final int min, max;

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
