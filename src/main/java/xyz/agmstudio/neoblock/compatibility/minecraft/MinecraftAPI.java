package xyz.agmstudio.neoblock.compatibility.minecraft;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftAPI {
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    public static final IEventBus EVENT_BUS = NeoForge.EVENT_BUS;

    public static boolean isLoaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    public static @NotNull ResourceLocation parseResourceLocation(String name) {
        return ResourceLocation.parse(name);
    }
    public static Optional<ResourceLocation> getResourceLocation(String name) {
        return Optional.ofNullable(ResourceLocation.tryParse(name));
    }
    public static ResourceLocation createResourceLocation(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static Optional<Item> getItem(String name) {
        return getItem(getResourceLocation(name).get());
    }
    public static Optional<Item> getItem(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.get(location));
    }
    public static Optional<ResourceLocation> getItemResource(Item item) {
        if (item == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return getItemResource(item).get() == location;
    }
    public static Optional<Block> getBlock(String name) {
        return getBlock(getResourceLocation(name).get());
    }
    public static Optional<Block> getBlock(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.get(location));
    }
    public static Optional<ResourceLocation> getBlockResource(Block block) {
        if (block == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.getKey(block));
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return getBlockResource(block).get() == location;
    }
    public static Optional<BlockState> getBlockState(String name) {
        return getBlockState(getResourceLocation(name).get());
    }
    public static Optional<BlockState> getBlockState(ResourceLocation location) {
        return getBlock(location).map(Block::defaultBlockState);
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return getBlockResource(state.getBlock()).get() == location;
    }
    public static Optional<EntityType<?>> getEntityType(String name) {
        return getEntityType(getResourceLocation(name).get());
    }
    public static Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(location));
    }
    public static Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        if (type == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return getEntityTypeResource(entityType).get() == location;
    }

    public static final class Collection {
        public static <T> void shuffle(List<T> list) {
            @NotNull RandomSource rand = WorldData.getRandom();
            for (int i = list.size(); i > 1; i--) Collections.swap(list, i - 1, rand.nextInt(i));
        }
    }

    public static abstract class AbstractWorldData extends SavedData {
        private static final String DATA_NAME = "neo_block_data";
        public static @NotNull WorldData load(@NotNull ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new Factory<>(
                            () -> WorldData.create(level),
                            (tag, provider) -> WorldData.load(tag, level)
                    ), DATA_NAME);
        }

        public abstract @NotNull CompoundTag save(@NotNull CompoundTag tag);

        @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
            return save(tag);
        }
    }

}
