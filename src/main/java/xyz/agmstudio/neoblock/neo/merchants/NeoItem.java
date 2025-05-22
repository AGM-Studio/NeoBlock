package xyz.agmstudio.neoblock.neo.merchants;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

public class NeoItem {
    private static final ResourceLocation air = MinecraftUtil.getResourceLocation("minecraft:air");
    private static boolean isValid(Item item) {
        @Nullable ResourceLocation resource = MinecraftUtil.getItemResource(item);
        return resource != null && resource != air;
    }

    public static NeoItem parse(String value) {
        value = value.toLowerCase();
        if (value.startsWith("mob:")) return MobItem.parse(value.substring(4));
        Pair<Item, Range> parsed = StringUtil.parseItem(value);
        if (isValid(parsed.getKey())) return new NeoItem(parsed.getKey(), parsed.getValue());
        NeoBlockMod.LOGGER.warn("Invalid item: {}", value);
        return null;
    }

    protected final Item item;
    protected final Range count;

    public NeoItem(Item item, Range count) {
        this.item = item;
        this.count = count;
    }

    public Item getItem() {
        return item;
    }
    public Range getCount() {
        return count;
    }
    public ItemStack modify(ItemStack stack) {
        return stack;
    }

    public String toString() {
        return count.toString() + MinecraftUtil.getItemResource(item);
    }

    public static class MobItem extends NeoItem {
        private final EntityType<?> mob;

        public static MobItem parse(String value) {
            Pair<EntityType<?>, Range> parsed = StringUtil.parseEntityType(value);
            Item item = StringUtil.parseItem(value + "_spawn_egg").getKey();
            if (isValid(item)) return new MobItem(item, parsed.getValue(), parsed.getKey());
            NeoBlockMod.LOGGER.warn("Unable to find the spawn egg: {}", value);
            return new MobItem(Items.EGG, parsed.getValue(), parsed.getKey());
        }

        public MobItem(Item item, Range count, EntityType<?> mob) {
            super(item, count);
            this.mob = mob;
        }

        @Override
        public ItemStack modify(ItemStack stack) {
            CompoundTag tag = MinecraftUtil.Items.getItemTag(stack);

            @Nullable ResourceLocation location = MinecraftUtil.getEntityTypeResource(mob);
            tag.putBoolean("isNeoMob", true);
            tag.putString("neoMobType", location != null ? location.toString() : "minecraft:pig");

            MinecraftUtil.Items.setItemTag(stack, tag);
            return stack;
        }

        public String toString() {
            return "mob:" + count.toString() + MinecraftUtil.getEntityTypeResource(mob);
        }
    }
}