package xyz.agmstudio.neoblock.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.tiers.merchants.NeoItem;

import java.nio.file.Path;
import java.util.Optional;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftUtil {
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    public static final IEventBus EVENT_BUS = NeoForge.EVENT_BUS;

    public static @Nullable ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static @Nullable Item getItem(String name) {
        return getItem(getResourceLocation(name));
    }
    public static @Nullable Item getItem(ResourceLocation location) {
        if (location == null) return null;
        return BuiltInRegistries.ITEM.get(location);
    }
    public static @Nullable ResourceLocation getItemResource(Item item) {
        if (item == null) return null;
        return BuiltInRegistries.ITEM.getKey(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return item != null && getItemResource(item) == location;
    }
    public static @Nullable Block getBlock(String name) {
        return getBlock(getResourceLocation(name));
    }
    public static @Nullable Block getBlock(ResourceLocation location) {
        if (location == null) return null;
        return BuiltInRegistries.BLOCK.get(location);
    }
    public static @Nullable ResourceLocation getBlockResource(Block block) {
        if (block == null) return null;
        return BuiltInRegistries.BLOCK.getKey(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return block != null && getBlockResource(block) == location;
    }
    public static @Nullable BlockState getBlockState(String name) {
        return getBlockState(getResourceLocation(name));
    }
    public static @Nullable BlockState getBlockState(ResourceLocation location) {
        if (location == null) return null;
        return getBlock(location).defaultBlockState();
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return state != null && getBlockResource(state.getBlock()).equals(location);
    }
    public static @Nullable EntityType<?> getEntityType(String name) {
        return getEntityType(getResourceLocation(name));
    }
    public static @Nullable EntityType<?> getEntityType(ResourceLocation location) {
        if (location == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.get(location);
    }
    public static @Nullable ResourceLocation getEntityTypeResource(EntityType<?> type) {
        if (type == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return entityType != null && getEntityTypeResource(entityType) == location;
    }

    public static final class Items {
        public static @NotNull CompoundTag getItemTag(ItemStack item) {
            CustomData data = item.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            return data.copyTag();
        }

        public static void setItemTag(@NotNull ItemStack item, CompoundTag tag) {
            item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static final class Entities {
        public static void leash(Entity mob, Entity to) {
            if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
        }
    }

    public static final class Merchant {
        public static MerchantOffer getOfferOf(NeoItem costA, NeoItem costB, NeoItem result, Range uses) {
            ItemStack r = new ItemStack(result.getItem(), result.getCount().get());
            ItemCost a = new ItemCost(costA.getItem(), costA.getCount().get());
            Optional<ItemCost> b = costB != null ?
                    Optional.of(new ItemCost(costB.getItem(), costB.getCount().get())) :
                    Optional.empty();

            return new MerchantOffer(a, b, r, uses.get(), 0, 0);
        }
    }
}
