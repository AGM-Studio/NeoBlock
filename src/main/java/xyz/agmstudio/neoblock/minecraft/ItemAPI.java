package xyz.agmstudio.neoblock.minecraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public final class ItemAPI {
    public static @NotNull CompoundTag getItemTag(ItemStack item) {
        CustomData data = item.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    public static void setItemTag(@NotNull ItemStack item, CompoundTag tag) {
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
