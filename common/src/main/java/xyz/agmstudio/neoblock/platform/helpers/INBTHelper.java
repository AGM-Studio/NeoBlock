package xyz.agmstudio.neoblock.platform.helpers;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public interface INBTHelper {
    final class IO {
        public static void write(Path file, CompoundTag nbt) throws IOException {
            OutputStream os = Files.newOutputStream(file);
            NeoBlock.NBT_HELPER.writeCompressed(nbt, os);
        }

        public static CompoundTag read(Path file) throws IOException {
            InputStream is = Files.newInputStream(file);
            return NeoBlock.NBT_HELPER.readCompressed(is);
        }
    }
    final class Block {
        public static Tag writeBlockPos(BlockPos pos) {
            return NeoBlock.NBT_HELPER.writeBlockPos(pos);
        }
        public static Tag writeBlockState(BlockState state) {
            return NeoBlock.NBT_HELPER.writeBlockState(state);
        }
        public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
            return NeoBlock.NBT_HELPER.readBlockPos(tag, key, def);
        }
        public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
            return NeoBlock.NBT_HELPER.readBlockState(tag, key, level);
        }
        public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
            return NeoBlock.NBT_HELPER.getBlockEntity(blockEntity, level);
        }
        public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
            NeoBlock.NBT_HELPER.loadBlockEntity(be, tag, level);
        }
    }
    final class Item {
        public static CompoundTag getItemTag(@NotNull ItemStack item) {
            return NeoBlock.NBT_HELPER.getItemTag(item);
        }
        public static void setItemTag(@NotNull ItemStack item, @NotNull CompoundTag tag) {
            NeoBlock.NBT_HELPER.setItemTag(item, tag);
        }
    }
    final class JEI {
        public static IRecipeSlotBuilder addTooltip(IRecipeSlotBuilder builder, List<Component> components) {
            return NeoBlock.NBT_HELPER.addTooltip(builder, components);
        }
    }

    void writeCompressed(CompoundTag nbt, OutputStream os) throws IOException;
    CompoundTag readCompressed(File file) throws IOException;
    CompoundTag readCompressed(InputStream is) throws IOException;

    CompoundTag getItemTag(@NotNull ItemStack item);
    void setItemTag(@NotNull ItemStack item, @NotNull CompoundTag tag);

    Tag writeBlockPos(BlockPos pos);
    BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def);

    Tag writeBlockState(BlockState state);
    BlockState readBlockState(CompoundTag tag, String key, ServerLevel level);

    CompoundTag getBlockEntity(BlockEntity be, ServerLevel level);
    void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level);

    IRecipeSlotBuilder addTooltip(IRecipeSlotBuilder builder, List<Component> components);
}
