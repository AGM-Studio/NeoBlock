package xyz.agmstudio.neocore.providers;

import net.minecraft.world.item.ItemStack;

public final class ConstantItemStack implements ItemStackProvider {
    private final ItemStack stack;
    ConstantItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override public ItemStack getStack() {
        return stack;
    }
}