package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.providers.ItemStackProvider;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.util.StringUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoItemSpec implements ItemStackProvider {
    private static final Pattern PATTERN =
            StringUtil.RANGE.optional().then(StringUtil.NAMESPACE).then(StringUtil.CHANCE.optional()).build(false);
    private static final ResourceLocation DEFAULT = NeoMC.parseResourceLocation("minecraft:stone");
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

    @Override public ItemStack getStack() {
        int count = range.sample(WorldManager.getRandom());
        return modify(new ItemStack(getItem(), count));
    }

    public ItemStack getStackWithChance() {
        if (chance >= 1.0) return getStack();
        return (WorldManager.getRandom().nextDouble() <= chance) ? getStack() : null;
    }

    public ResourceLocation getResource() {
        return NeoMC.getItemResource(getItem()).orElse(DEFAULT);
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
    public ItemStack modifyForTrader(ItemStack item, NeoBlock block) {
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

        Item item = NeoMC.getItem(matcher.group("id")).orElse(null);
        if (item == null) return Optional.empty();

        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));

        return Optional.of(new NeoItemSpec(item, range, chance));
    }
}