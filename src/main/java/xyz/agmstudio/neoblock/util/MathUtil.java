package xyz.agmstudio.neoblock.util;

public class MathUtil {
    /**
     * A class to hold math compatibility for 1.20.1... is not used in newer versions though...
     */
    public static long clamp(long min, long max, long value) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
