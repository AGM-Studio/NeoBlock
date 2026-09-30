package xyz.agmstudio.neocore.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@ParametersAreNonnullByDefault
public interface INBT {
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

    ItemStack applyModifiers(ItemStack stack, @Nullable String dataString, ServerLevel level);
}