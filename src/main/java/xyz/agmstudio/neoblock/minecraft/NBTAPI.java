package xyz.agmstudio.neoblock.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NBTAPI {
    public static class IO {
        public static void write(Path file, CompoundTag nbt) throws IOException {
            OutputStream os = Files.newOutputStream(file);
            NbtIo.writeCompressed(nbt, os);
        }

        public static CompoundTag read(Path file) throws IOException {
            InputStream is = Files.newInputStream(file);
            return NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        }
    }

    public static Tag writeBlockPos(BlockPos pos) {
        return NbtUtils.writeBlockPos(pos);
    }

    public static Tag writeBlockState(BlockState state) {
        return NbtUtils.writeBlockState(state);
    }

    public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
        return NbtUtils.readBlockPos(tag, key).orElse(def);
    }

    public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
        return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound(key));
    }

    public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
        if (blockEntity == null) return null;
        return blockEntity.saveWithFullMetadata(level.registryAccess());
    }

    public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
        if (be == null || tag == null) return;
        be.loadWithComponents(tag, level.registryAccess());
        be.setChanged();
    }
}
