package xyz.agmstudio.neoblock.tiers.merchants;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import org.apache.commons.lang3.tuple.Pair;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.util.StringUtil;

public class NeoItem {
    private static final ResourceLocation air = ResourceLocation.parse("minecraft:air");
    private static boolean isAir(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).equals(air);
    }

    public static NeoItem parse(String value) {
        value = value.toLowerCase();
        if (value.startsWith("mob:")) return MobItem.parse(value.substring(4));
        Pair<Item, Range> parsed = StringUtil.parseItem(value);
        if (isAir(parsed.getKey())) return null;
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
    public ItemCost getCost() {
        return new ItemCost(item, count.get());
    }
    
    public String toString() {
        return count.toString() + BuiltInRegistries.ITEM.getKey(item);
    }

    public static class MobItem extends NeoItem {
        private final EntityType<?> mob;

        public static MobItem parse(String value) {
            Pair<EntityType<?>, Range> parsed = StringUtil.parseEntityType(value);
            Item item = StringUtil.parseItem(value + "_spawn_egg").getKey();
            if (isAir(item)) item = Items.BEE_SPAWN_EGG;
            return new MobItem(item, parsed.getValue(), parsed.getKey());
        }

        public MobItem(Item item, Range count, EntityType<?> mob) {
            super(item, count);
            this.mob = mob;
        }

        @Override
        public ItemStack getStack() {
            ItemStack item = super.getStack();
            CustomData data = item.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            data = data.update(tag -> {
                tag.putBoolean("isNeoMob", true);
                tag.putString("neoMobType", BuiltInRegistries.ENTITY_TYPE.getKey(mob).toString());
            });

            item.set(DataComponents.CUSTOM_DATA, data);

            return item;
        }

        public String toString() {
            return "mob:" + count.toString() + BuiltInRegistries.ENTITY_TYPE.getKey(mob);
        }
    }
}