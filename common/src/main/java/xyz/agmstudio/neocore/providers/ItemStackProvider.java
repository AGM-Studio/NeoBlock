package xyz.agmstudio.neocore.providers;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemStackProvider {
    ItemStack getStack();

    static @NotNull ConstantItemStack of(ItemStack stack) {
        return new ConstantItemStack(stack);
    }
}