package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.platform.helpers.IMinecraftHelper;

import java.util.Optional;

public final class NeoForgeMinecraftHelper implements IMinecraftHelper {
    @Override public @NotNull ResourceLocation parseResourceLocation(String name) {
        return ResourceLocation.parse(name);
    }
    @Override public Optional<ResourceLocation> getResourceLocation(String name) {
        return Optional.ofNullable(ResourceLocation.tryParse(name));
    }
    @Override public ResourceLocation createResourceLocation(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override public Optional<Item> getItem(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.get(location));
    }
    @Override public Optional<ResourceLocation> getItemResource(Item item) {
        if (item == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }

    @Override public Optional<Block> getBlock(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.get(location));
    }
    @Override public Optional<ResourceLocation> getBlockResource(Block block) {
        if (block == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.getKey(block));
    }

    @Override public Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(location));
    }
    @Override public Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        if (type == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
}
