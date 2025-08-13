package xyz.agmstudio.neoblock.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.helpers.IMinecraftHelper;

import java.util.Optional;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftUtil {
    private static final IMinecraftHelper HELPER = Services.load(IMinecraftHelper.class);

    public static @NotNull ResourceLocation parseResourceLocation(String name) {
        return HELPER.parseResourceLocation(name);
    }
    public static Optional<ResourceLocation> getResourceLocation(String name) {
        return HELPER.getResourceLocation(name);
    }
    public static ResourceLocation createResourceLocation(String namespace, String path) {
        return HELPER.createResourceLocation(namespace, path);
    }

    public static Optional<Item> getItem(String name) {
        return getItem(getResourceLocation(name).orElse(null));
    }
    public static Optional<Item> getItem(ResourceLocation location) {
        return HELPER.getItem(location);
    }
    public static Optional<ResourceLocation> getItemResource(Item item) {
        return HELPER.getItemResource(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return getItemResource(item).orElse(null) == location;
    }

    public static Optional<Block> getBlock(String name) {
        return getBlock(getResourceLocation(name).orElse(null));
    }
    public static Optional<Block> getBlock(ResourceLocation location) {
        return HELPER.getBlock(location);
    }
    public static Optional<ResourceLocation> getBlockResource(Block block) {
        return HELPER.getBlockResource(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return getBlockResource(block).orElse(null) == location;
    }

    public static Optional<BlockState> getBlockState(String name) {
        return getBlockState(getResourceLocation(name).orElse(null));
    }
    public static Optional<BlockState> getBlockState(ResourceLocation location) {
        return getBlock(location).map(Block::defaultBlockState);
    }
    public static Optional<ResourceLocation> getBlockStateResource(BlockState state) {
        return getBlockResource(state.getBlock());
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return getBlockResource(state.getBlock()).orElse(null) == location;
    }

    public static Optional<EntityType<?>> getEntityType(String name) {
        return getEntityType(getResourceLocation(name).orElse(null));
    }
    public static Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        return HELPER.getEntityType(location);
    }
    public static Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        return HELPER.getEntityTypeResource(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return getEntityTypeResource(entityType).orElse(null) == location;
    }
}
