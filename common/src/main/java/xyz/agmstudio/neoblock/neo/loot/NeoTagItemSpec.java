package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.platform.IConfig;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTagItemSpec extends NeoItemSpec {
    private static final Pattern TAG_PATTERN =
            StringUtil.RANGE.optional().then("#").then(StringUtil.NAMESPACE).then(StringUtil.CHANCE.optional()).build(false);
    private static final HashMap<String, List<NeoItemSpec>> MAP = new HashMap<>();

    public static void reloadTags() {
        IConfig config = NeoBlockMod.get().getConfig("tags");
        IConfig section = config != null ? config.getSection("items") : null;
        if (section == null) {
            NeoBlockMod.getLogger().error("Failed to load item tags from configs.");
            return;
        }

        section.forEach((key, value) -> {
            List<String> list = section.get(key);
            if (list == null || list.isEmpty()) {
                NeoBlockMod.getLogger().warn("Failed to load items from items.{}.", key);
                return;
            }

            List<NeoItemSpec> result = new ArrayList<>();
            list.forEach(item -> NeoItemSpec.parseItem(item).ifPresent(result::add));

            MAP.put(key, result);
            NeoBlockMod.getLogger().info("Loaded {} items for tag #neoblock:{}", list.size(), key);
        });
    }

    private final Supplier<ItemStack> supplier;
    private final ResourceLocation location;

    public NeoTagItemSpec(TagKey<Item> tag, UniformInt range, double chance) {
        super(Items.DIRT, range, chance);
        this.location = tag.location();
        this.supplier = () -> this.ofTag(tag);
    }
    private ItemStack ofTag(TagKey<Item> tag) {
        List<Item> items = NeoMC.getItemsOfTag(tag);
        Optional<Item> item = WorldManager.getRandomItem(items);
        if (item.isEmpty()) {
            NeoBlockMod.getLogger().warn("Tag key {} has no items to choose from.", location);
            return NeoItemSpec.getDefault();
        }
        return new ItemStack(item.get(), range.sample(WorldManager.getRandom()));
    }

    public NeoTagItemSpec(String name, UniformInt range, double chance) {
        super(Items.DIRT, range, chance);
        this.location = NeoMC.createResourceLocation(NeoBlockMod.MOD_ID, name);

        List<NeoItemSpec> list = MAP.getOrDefault(name, List.of());
        this.supplier = () -> this.ofList(list);
    }
    private ItemStack ofList(List<NeoItemSpec> list) {
        Optional<NeoItemSpec> item = WorldManager.getRandomItem(list);
        if (item.isEmpty()) {
            NeoBlockMod.getLogger().warn("Custom list {} has no items to choose from.", list);
            return NeoItemSpec.getDefault();
        }
        return item.get().getStack();
    }

    @Override public ItemStack getStack() {
        return supplier.get();
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

        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));

        ResourceLocation location = NeoMC.parseResourceLocation(matcher.group("id"));
        if (location.getNamespace().equals(NeoBlockMod.MOD_ID)) {
            if (MAP.getOrDefault(location.getPath(), List.of()).isEmpty()) {
                NeoBlockMod.getLogger().warn("Tag item #{} is empty.", location);
                return Optional.empty();
            }

            return Optional.of(new NeoTagItemSpec(location.getPath(), range, chance));
        }

        TagKey<Item> tag = TagKey.create(Registries.ITEM, location);
        return Optional.of(new NeoTagItemSpec(tag, range, chance));
    }
}