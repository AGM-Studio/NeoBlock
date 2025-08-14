package xyz.agmstudio.neoblock.platform;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.platform.helpers.INBTHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class ForgeNBTHelper implements INBTHelper {
    @Override public void writeCompressed(CompoundTag nbt, OutputStream os) throws IOException {
        NbtIo.writeCompressed(nbt, os);
    }

    @Override public CompoundTag readCompressed(File file) throws IOException {
        return NbtIo.readCompressed(file);
    }

    @Override public CompoundTag readCompressed(InputStream is) throws IOException {
        return NbtIo.readCompressed(is);
    }

    @Override public CompoundTag getItemTag(@NotNull ItemStack item) {
        if (item.hasTag()) return item.getOrCreateTag();
        return new CompoundTag();
    }

    @Override public void setItemTag(@NotNull ItemStack item, @NotNull CompoundTag tag) {
        item.setTag(tag);
    }

    @Override public Tag writeBlockPos(BlockPos pos) {
        return NbtUtils.writeBlockPos(pos);
    }

    @Override public Tag writeBlockState(BlockState state) {
        return NbtUtils.writeBlockState(state);
    }

    @Override public BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
        CompoundTag blockTag = tag.getCompound(key);
        if (blockTag.isEmpty()) return def;
        return NbtUtils.readBlockPos(tag);
    }

    @Override public BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound(key));
    }

    @Override public CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
        if (blockEntity == null) return null;
        return blockEntity.saveWithFullMetadata();
    }

    @Override public void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
        if (be == null || tag == null) return;
        be.load(tag);
        be.setChanged();
    }

    @Override public IRecipeSlotBuilder addTooltip(IRecipeSlotBuilder builder, List<Component> components) {
        return builder.addTooltipCallback((view, tooltip) -> {
            tooltip.addAll(components);
        });
    }
}
