package xyz.agmstudio.neoblock.platform.helpers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface IMinecraftHelper {
    @NotNull ResourceLocation parseResourceLocation(String name);
    Optional<ResourceLocation> getResourceLocation(String name);
    ResourceLocation createResourceLocation(String namespace, String path);

    Optional<Item> getItem(ResourceLocation location);
    Optional<ResourceLocation> getItemResource(Item item);

    Optional<Block> getBlock(ResourceLocation location);
    Optional<ResourceLocation> getBlockResource(Block block);

    Optional<EntityType<?>> getEntityType(ResourceLocation location);
    Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type);
}
