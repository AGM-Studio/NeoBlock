package xyz.agmstudio.neoblock.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftUtil {
    public static @Nullable ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static @Nullable Item getItem(String name) {
        return getItem(getResourceLocation(name));
    }
    public static @Nullable Item getItem(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.ITEMS.getValue(location);
    }
    public static @Nullable ResourceLocation getItemResource(Item item) {
        if (item == null) return null;
        return ForgeRegistries.ITEMS.getKey(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return item != null && getItemResource(item) == location;
    }
    public static @Nullable Block getBlock(String name) {
        return getBlock(getResourceLocation(name));
    }
    public static @Nullable Block getBlock(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.BLOCKS.getValue(location);
    }
    public static @Nullable ResourceLocation getBlockResource(Block block) {
        if (block == null) return null;
        return ForgeRegistries.BLOCKS.getKey(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return block != null && getBlockResource(block) == location;
    }
    public static @Nullable BlockState getBlockState(String name) {
        return getBlockState(getResourceLocation(name));
    }
    public static @Nullable BlockState getBlockState(ResourceLocation location) {
        if (location == null) return null;
        Block block = getBlock(location);
        return block != null ? block.defaultBlockState() : null;
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return state != null && getBlockResource(state.getBlock()) == location;
    }
    public static @Nullable EntityType<?> getEntityType(String name) {
        return getEntityType(getResourceLocation(name));
    }
    public static @Nullable EntityType<?> getEntityType(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(location);
    }
    public static @Nullable ResourceLocation getEntityTypeResource(EntityType<?> type) {
        if (type == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getKey(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return entityType != null && getEntityTypeResource(entityType) == location;
    }

    public static final class Items {
        public static @NotNull CompoundTag getItemTag(ItemStack item) {
            return item.getOrCreateTag();
        }

        public static void setItemTag(@NotNull ItemStack item, CompoundTag tag) {
            item.setTag(tag);
        }
    }

    public static final class Entities {
        public static void leash(Entity mob, Entity to) {
            if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
        }
    }
}
