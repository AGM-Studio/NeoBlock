package xyz.agmstudio.neoblock.util;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;

import java.util.AbstractMap;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    private static final Pattern BLOCK_PATTERN = Pattern.compile("^(?:(?<count>\\d+)x)?(?<id>[a-z0-9_]+:[a-z0-9_/]+)$");
    private static final UniformInt ONE = UniformInt.of(1, 1);

    /**
     * Converts string to snake-case
     *
     * @param string the string to be converted
     * @return the snake-case
     */
    public static @NotNull String convertToSnakeCase(@NotNull String string) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('-');
                result.append(Character.toLowerCase(c));
            } else result.append(c);
        }
        return result.toString();
    }

    /**
     * Encodes a string into Base64 format.
     *
     * @param input the string to be encoded
     * @return the Base64 encoded string
     */
    public static @NotNull String encodeToBase64(@NotNull String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    /**
     * Converts amount of ticks into a readable format
     *
     * @param ticks the amount of ticks
     * @return readable time format
     */
    public static @NotNull String formatTicks(long ticks) {
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder timeString = new StringBuilder();
        if (hours > 0) {
            timeString.append(hours).append(":");
            timeString.append(String.format("%02d:", minutes));
            timeString.append(String.format("%02d", seconds));
        } else if (minutes > 0) {
            timeString.append(minutes).append(":");
            timeString.append(String.format("%02d", seconds));
        } else timeString.append(seconds).append(" seconds");
        return timeString.toString().trim();
    }
    public static @NotNull String formatTicks(int ticks) {
        return formatTicks((long) ticks);
    }

    /**
     * Formats the double into the digits requested.
     *
     * @param value the value to be rounded
     * @param digits the amount of digits
     * @return formatted text
     */
    public static @NotNull String round(double value, int digits) {
        return String.format("%%.%df".formatted(digits), value);
    }

    /**
     * Formats the double into percentage with specified digits
     *
     * @param value  the value to turn into percentage
     * @param digits the amount of digits
     * @return formatted text
     */
    public static @NotNull String percentage(double value, int digits) {
        return round(value * 100.0, digits) + "%";
    }

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
        if (string == null) return ONE;
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

    public static Optional<Map.Entry<Block, Integer>> parseBlock(String input) {
        Matcher matcher = BLOCK_PATTERN.matcher(input.trim());

        if (!matcher.matches()) {
            NeoBlockMod.LOGGER.warn("Invalid block: '{}'", input);
            return Optional.empty();
        }

        Optional<Block> block = MinecraftAPI.getBlock(matcher.group("id"));
        if (block.isEmpty()) {
            NeoBlockMod.LOGGER.warn("Unknown block ID: '{}'", matcher.group("id"));
            return Optional.empty();
        }

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new AbstractMap.SimpleEntry<>(block.get(), count));
    }
}
