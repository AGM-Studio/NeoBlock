package xyz.agmstudio.neoblock.platform.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import xyz.agmstudio.neoblock.platform.Services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public interface INBTHelper {
    final class IO {
        public static void write(Path file, CompoundTag nbt) throws IOException {
            OutputStream os = Files.newOutputStream(file);
            Services.NBT.writeCompressed(nbt, os);
        }

        public static CompoundTag read(Path file) throws IOException {
            InputStream is = Files.newInputStream(file);
            return Services.NBT.readCompressed(is);
        }
    }
    final class Block {
        public static Tag writeBlockPos(BlockPos pos) {
            return Services.NBT.writeBlockPos(pos);
        }
        public static Tag writeBlockState(BlockState state) {
            return Services.NBT.writeBlockState(state);
        }
        public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
            return Services.NBT.readBlockPos(tag, key, def);
        }
        public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
            return Services.NBT.readBlockState(tag, key, level);
        }
        public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
            return Services.NBT.getBlockEntity(blockEntity, level);
        }
        public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
            Services.NBT.loadBlockEntity(be, tag, level);
        }
    }

    Tag writeBlockPos(BlockPos pos);
    BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def);

    Tag writeBlockState(BlockState state);
    BlockState readBlockState(CompoundTag tag, String key, ServerLevel level);

    CompoundTag getBlockEntity(BlockEntity be, ServerLevel level);
    void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level);

    void writeCompressed(CompoundTag nbt, OutputStream os) throws IOException;
    CompoundTag readCompressed(File file) throws IOException;
    CompoundTag readCompressed(InputStream is) throws IOException;
}
