package xyz.agmstudio.neoblock.util;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaUtil {
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

    public static <T> void shuffle(List<T> list) {
        @NotNull RandomSource rand = WorldData.getRandom();
        for (int i = list.size(); i > 1; i--) Collections.swap(list, i - 1, rand.nextInt(i));
    }

    public static <T> List<T> reverse(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(iter -> list.add(0, iter));
        return list;
    }
}
