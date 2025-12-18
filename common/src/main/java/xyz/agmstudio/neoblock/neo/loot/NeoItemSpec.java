package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.PatternUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoItemSpec {
    private static final Pattern PATTERN =
            PatternUtil.RANGE.optional().then(PatternUtil.NAMESPACE).then(PatternUtil.CHANCE.optional()).build(false);
    private static final ResourceLocation DEFAULT = MinecraftUtil.parseResourceLocation("minecraft:stone");
    protected static ItemStack getDefault() {
        return new ItemStack(Items.STONE, 1);
    }

    protected final Item item;
    protected final UniformInt range;
    protected final double chance;

    public NeoItemSpec(Item item, UniformInt range, double chance) {
        this.item = item;
        this.range = range;
        this.chance = Math.min(Math.max(chance, 0.0), 1.0);
    }


    public ItemStack getStack() {
        int count = range.sample(WorldData.getRandom());
        return modify(new ItemStack(getItem(), count));
    }

    public ItemStack getStackWithChance() {
        if (chance >= 1.0) return getStack();
        return (WorldData.getRandom().nextDouble() <= chance) ? getStack() : null;
    }

    public ResourceLocation getResource() {
        return MinecraftUtil.getItemResource(getItem()).orElse(DEFAULT);
    }
    public String getId() {
        return getResource().toString();
    }
    public Item getItem() {
        return item;
    }
    public UniformInt getRange() {
        return range;
    }
    public double getChance() {
        return chance;
    }

    public ItemStack modify(ItemStack item) {
        return item;
    }

    @Override public String toString() {
        return StringUtil.stringUniformInt(range) + getId() + StringUtil.stringChance(chance);
    }

    public static Optional<? extends NeoItemSpec> parseItem(String input) {
        if (input == null) return Optional.empty();

        Optional<NeoMobSpec> mob = NeoMobSpec.parseMob(input);
        if (mob.isPresent()) return mob;

        Optional<NeoTagItemSpec> tag = NeoTagItemSpec.parseTagItem(input);
        if (tag.isPresent()) return tag;

        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        Item item = MinecraftUtil.getItem(matcher.group("id")).orElse(null);
        if (item == null) return Optional.empty();

        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));

        return Optional.of(new NeoItemSpec(item, range, chance));
    }
}