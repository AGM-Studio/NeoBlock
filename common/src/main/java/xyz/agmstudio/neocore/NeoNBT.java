package xyz.agmstudio.neocore;

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
import xyz.agmstudio.neocore.platform.INBT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class NeoNBT {
    private static final INBT nbt = NeoMod.MC.getINBT();

    public static final class IO {
        public static void write(Path file, CompoundTag nbt) throws IOException {
            OutputStream os = Files.newOutputStream(file);
            NeoNBT.nbt.writeCompressed(nbt, os);
        }

        public static CompoundTag read(Path file) throws IOException {
            InputStream is = Files.newInputStream(file);
            return nbt.readCompressed(is);
        }
    }

    public static final class Block {
        public static Tag writeBlockPos(BlockPos pos) {
            return nbt.writeBlockPos(pos);
        }

        public static Tag writeBlockState(BlockState state) {
            return nbt.writeBlockState(state);
        }

        public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
            return nbt.readBlockPos(tag, key, def);
        }

        public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
            return nbt.readBlockState(tag, key, level);
        }

        public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
            return nbt.getBlockEntity(blockEntity, level);
        }

        public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
            nbt.loadBlockEntity(be, tag, level);
        }
    }

    public static final class Item {
        public static CompoundTag getItemTag(@NotNull ItemStack item) {
            return nbt.getItemTag(item);
        }

        public static void setItemTag(@NotNull ItemStack item, @NotNull CompoundTag tag) {
            nbt.setItemTag(item, tag);
        }
    }

    public static final class JEI {
        public static IRecipeSlotBuilder addTooltip(IRecipeSlotBuilder builder, List<Component> components) {
            return nbt.addTooltip(builder, components);
        }
    }
}