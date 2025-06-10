package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.util.valueproviders.UniformInt;

public final class NeoParser {
    public static double parseChance(String string) {
        if (string != null) try {
            return Double.parseDouble(string) / 100.0;
        } catch (NumberFormatException ignored) {}
        return 1.0;
    }
    public static String stringChance(double chance) {
        return (chance < 1.0) ? " " + (chance * 100) + "%" : "";
    }

    public static UniformInt parseRange(String string) {
        if (string == null) return NeoItemStack.ONE;
        if (string.endsWith("x")) string = string.substring(0, string.length() - 1);
        if (string.contains("-")) {
            String[] parts = string.split("-");
            return UniformInt.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        int c = Integer.parseInt(string);
        return UniformInt.of(c, c);
    }
    public static String stringUniformInt(UniformInt range) {
        int min = range.getMinValue();
        int max = range.getMaxValue();
        return (min == max) ? min <= 1 ? "" : min + "x" : min + "-" + max + "x";
    }
}
