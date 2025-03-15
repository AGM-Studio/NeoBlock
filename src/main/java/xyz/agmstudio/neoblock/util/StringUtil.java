package xyz.agmstudio.neoblock.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import xyz.agmstudio.neoblock.data.Range;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    private static final Pattern NX = Pattern.compile("(\\d+)x");

    public static Pair<String, Range> parseCount(String value, Range defaultRange) {
        Matcher matcher = NX.matcher(value);
        if (matcher.lookingAt()) {
            defaultRange = Range.parse(matcher.group(1));
            value = value.substring(0, matcher.group(1).length());
        }
        return Pair.of(value, defaultRange);
    }

    public static Pair<Block, Range> parseBlock(String value) {
        Pair<String, Range> parsed = parseCount(value, new Range(1));
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(parsed.getLeft()));
        return Pair.of(block, parsed.getRight());
    }
    public static Pair<Item, Range> parseItem(String value) {
        Pair<String, Range> parsed = parseCount(value, new Range(1));
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(parsed.getLeft()));
        return Pair.of(item, parsed.getRight());
    }
}
