package xyz.agmstudio.neoblock.util;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.data.Range;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
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


    private static final Pattern RX = Pattern.compile("^(\\d+)(?:-(\\d+))?x");
    private static final Pattern RP = Pattern.compile("^(\\d+)(?:-(\\d+))?");

    /**
     * Parses a count prefix from the string and returns a Pair of the remaining identifier string
     * and the parsed Range. If no count prefix is found, the defaultRange is used.
     * <p>
     * For example, given "2-3xminecraft:stone", this method returns a pair of:
     *   - "minecraft:stone"
     *   - Range with min=2 and max=3.
     *
     * @param value the input string (e.g. "2-3xminecraft:stone")
     * @param defaultRange the range to use if no count prefix is found
     * @return a Pair of the identifier (String) and the Range
     */
    public static Pair<String, Range> parseCount(String value, Range defaultRange) {
        value = value.strip();
        Matcher matcher = RX.matcher(value);
        Range parsedRange = defaultRange;
        if (matcher.find()) {
            String countPart = matcher.group(0);
            Range temp = parseRange(countPart.substring(0, countPart.length() - 1));
            if (temp != null) parsedRange = temp;
            value = value.substring(matcher.end());
        }
        return Pair.of(value, parsedRange);
    }

    /**
     * Parses a numeric range from the given string.
     * For example:
     *   - "1" returns Range(1, 1)
     *   - "1-5" returns Range(1, 5)
     *
     * @param value the input string representing the range
     * @return a Range object, or null if the string does not match
     */
    public static @Nullable Range parseRange(String value) {
        value = value.strip();
        Matcher matcher = RP.matcher(value);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : min;
            return new Range(min, max);
        }
        return null;
    }

    /**
     * Parses a Block from the given string.
     * The string should be in the format "[count]x[block_id]", for example "2xminecraft:stone" or "3-5xminecraft:stone".
     *
     * @param value the input string
     * @return a Pair of the Block and the Range extracted
     */
    public static Pair<Block, Range> parseBlock(String value) {
        Pair<String, Range> parsed = parseCount(value, new Range(1, 1));
        Block block = MinecraftUtil.getBlock(parsed.getLeft());
        return Pair.of(block, parsed.getRight());
    }

    /**
     * Parses an Item from the given string.
     * The string should be in the format "[count]x[item_id]", for example "1xminecraft:diamond" or "2-3xminecraft:diamond".
     *
     * @param value the input string
     * @return a Pair of the Item and the Range extracted
     */
    public static Pair<Item, Range> parseItem(String value) {
        Pair<String, Range> parsed = parseCount(value, new Range(1, 1));
        Item item = MinecraftUtil.getItem(parsed.getLeft());
        return Pair.of(item, parsed.getRight());
    }

    /**
     * Parses an EntityType from the given string.
     * The string should be in the format "[count]x[entity_type]", for example "1xminecraft:pig" or "2-4xminecraft:pig".
     *
     * @param value the input string
     * @return a Pair of the EntityType and the Range extracted
     */
    public static Pair<EntityType<?>, Range> parseEntityType(String value) {
        Pair<String, Range> parsed = parseCount(value, new Range(1, 1));
        EntityType<?> type = MinecraftUtil.getEntityType(parsed.getLeft());
        return Pair.of(type, parsed.getRight());
    }
}
