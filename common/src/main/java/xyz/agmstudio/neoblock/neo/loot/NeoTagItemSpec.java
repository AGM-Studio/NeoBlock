package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTagItemSpec extends NeoItemSpec {
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "(?:(?<count>\\d+(?:-\\d+)?)x)?#(?<id>[\\w:]+)(?:\\s+(?<chance>\\d+(?:\\.\\d*)?)%?)?"
    );

    private final Supplier<List<Item>> supplier;
    private final ResourceLocation location;

    public NeoTagItemSpec(TagKey<Item> tag, UniformInt range, double chance) {
        super(Items.DIRT, range, chance);
        this.location = tag.location();
        this.supplier = () -> MinecraftUtil.getItemsOfTag(tag);
    }

    public NeoTagItemSpec(List<Item> list, String name, UniformInt range, double chance) {
        super(Items.DIRT, range, chance);
        this.location = MinecraftUtil.createResourceLocation(NeoBlock.MOD_ID, name);
        this.supplier = () -> new ArrayList<>(list);
    }

    @Override public Item getItem() {
        List<Item> items = supplier.get();
        if (items.isEmpty()) {
            NeoBlock.LOGGER.warn("No item for #{} was found!", location);
            return super.getItem();
        }
        return items.get(WorldData.getRandom().nextInt(items.size()));
    }

    @Override public ResourceLocation getResource() {
        return location;
    }

    @Override public String getId() {
        return "#" + getResource();
    }

    @Override public String toString() {
        return StringUtil.stringUniformInt(range) + getId() + StringUtil.stringChance(chance);
    }

    public static Optional<NeoTagItemSpec> parseTagItem(String input) {
        Matcher matcher = TAG_PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        ResourceLocation tagLoc = MinecraftUtil.parseResourceLocation(matcher.group("id"));
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);

        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));
        return Optional.of(new NeoTagItemSpec(tag, range, chance));
    }
}
