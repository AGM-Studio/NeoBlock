package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.platform.helpers.INBTHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NeoForgeNBTHelper implements INBTHelper {
    @Override public void writeCompressed(CompoundTag nbt, OutputStream os) throws IOException {
        NbtIo.writeCompressed(nbt, os);
    }

    @Override public CompoundTag readCompressed(File file) throws IOException {
        return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
    }

    @Override public CompoundTag readCompressed(InputStream is) throws IOException {
        return NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
    }

    @Override public CompoundTag getItemTag(@NotNull ItemStack item) {
        CustomData data = item.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    @Override public void setItemTag(@NotNull ItemStack item, @NotNull CompoundTag tag) {
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public Tag writeBlockPos(BlockPos pos) {
        return NbtUtils.writeBlockPos(pos);
    }

    @Override public Tag writeBlockState(BlockState state) {
        return NbtUtils.writeBlockState(state);
    }

    @Override public BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
        return NbtUtils.readBlockPos(tag, key).orElse(def);
    }

    @Override public BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound(key));
    }

    @Override public CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
        if (blockEntity == null) return null;
        return blockEntity.saveWithFullMetadata(level.registryAccess());
    }

    @Override public void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
        if (be == null || tag == null) return;
        be.loadWithComponents(tag, level.registryAccess());
        be.setChanged();
    }
}
