package xyz.agmstudio.neoblock.platform;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neocore.platform.INBT;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@ParametersAreNonnullByDefault
public final class ForgeNBT implements INBT {
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

    @Override public CompoundTag getBlockEntity(@Nullable BlockEntity blockEntity, ServerLevel level) {
        if (blockEntity == null) return null;
        return blockEntity.saveWithFullMetadata();
    }

    @Override public void loadBlockEntity(@Nullable BlockEntity be, @Nullable CompoundTag tag, ServerLevel level) {
        if (be == null || tag == null) return;
        be.load(tag);
        be.setChanged();
    }

    @Override public ItemStack applyModifiers(ItemStack stack, @Nullable String dataString, ServerLevel level) {
        if (dataString == null || dataString.isBlank()) return stack;
        try {
            CompoundTag parsedTag = TagParser.parseTag(dataString);
            stack.getOrCreateTag().merge(parsedTag);
        } catch (CommandSyntaxException e) {
            NeoBlockMod.getLogger().error("Unable to apply data components to the given item: \n\tData: {}", dataString, e);
        }

        return stack;
    }
}