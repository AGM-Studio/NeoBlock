package xyz.agmstudio.neoblock.tiers.merchants;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.util.StringUtil;

public class NeoItem {
    private static final ResourceLocation air = ResourceLocation.tryParse("minecraft:air");
    private static boolean isNotValid(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null || air.equals(key);
    }

    public static NeoItem parse(String value) {
        value = value.toLowerCase();
        if (value.startsWith("mob:")) return MobItem.parse(value.substring(4));
        Pair<Item, Range> parsed = StringUtil.parseItem(value);
        if (isNotValid(parsed.getKey())) return null;
        return new NeoItem(parsed.getKey(), parsed.getValue());
    }

    protected final Item item;
    protected final Range count;

    public NeoItem(Item item, Range count) {
        this.item = item;
        this.count = count;
    }

    public ItemStack getStack() {
        return new ItemStack(item, count.get());
    }
    
    public String toString() {
        return count.toString() + ForgeRegistries.ITEMS.getKey(item);
    }

    public static class MobItem extends NeoItem {
        private final EntityType<?> mob;

        public static MobItem parse(String value) {
            Pair<EntityType<?>, Range> parsed = StringUtil.parseEntityType(value);
            Item item = StringUtil.parseItem(value + "_spawn_egg").getKey();
            if (isNotValid(item)) item = Items.BEE_SPAWN_EGG;
            return new MobItem(item, parsed.getValue(), parsed.getKey());
        }

        public MobItem(Item item, Range count, EntityType<?> mob) {
            super(item, count);
            this.mob = mob;
        }

        @Override
        public ItemStack getStack() {
            ItemStack item = super.getStack();
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob);
            if (key == null) return item;

            CompoundTag tag = item.getOrCreateTag();
            tag.putBoolean("isNeoMob", true);
            tag.putString("neoMobType", key.toString());

            item.setTag(tag);
            return item;
        }

        public String toString() {
            return "mob:" + count.toString() + ForgeRegistries.ENTITY_TYPES.getKey(mob);
        }
    }
}