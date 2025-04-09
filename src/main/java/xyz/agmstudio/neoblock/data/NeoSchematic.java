package xyz.agmstudio.neoblock.data;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NeoSchematic {
    public static void saveSchematic(ServerLevel level, BlockPos pos1, BlockPos pos2, @Nullable BlockPos center, @Nullable String name, CommandSourceStack source) {
        CompoundTag nbt = new NeoSchematic(level, pos1, pos2, center).toNBT();

        try {
            Path folder = level.getServer().getWorldPath(LevelResource.ROOT).resolve("schematics");
            Files.createDirectories(folder);

            if (name == null) {
                int i = 0;
                do name = "NeoBlockSchematic_" + i++ + ".nbt"; while (!folder.resolve(name).toFile().exists());
            } else if (!name.endsWith(".nbt")) {
                name = name + ".nbt";
            }

            Path file = folder.resolve(name);
            try (OutputStream os = Files.newOutputStream(file)) {
                NbtIo.writeCompressed(nbt, os);
            }

            source.sendSuccess(() -> Component.literal("Schematic saved to " + file.getFileName()), false);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to save schematic: " + e.getMessage()));
            NeoBlockMod.LOGGER.error("Failed to save schematic", e);
        }
    }
    public static void loadSchematic(ServerLevel level, BlockPos origin, @Nullable String name, CommandSourceStack source) {
        try {
            Path folder = level.getServer().getWorldPath(LevelResource.ROOT).resolve("schematics");

            if (name == null) {
                File[] files = folder.toFile().listFiles((file) -> file.getName().endsWith(".nbt"));
                String file = files != null && files.length > 0 ? files[0].getName() : "NeoBlockSchematic_0.nbt";
            } else if (!name.endsWith(".nbt")) {
                name = name + ".nbt";
            }

            Path file = name != null ? folder.resolve(name) : null;
            if (file == null || !Files.exists(file)) {
                source.sendFailure(Component.literal("Schematic file not found."));
                return;
            }

            CompoundTag tag;
            try (InputStream is = Files.newInputStream(file)) {
                tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
            }

            NeoSchematic data = NeoSchematic.fromNBT(tag, level);
            data.place(level, origin);

            source.sendSuccess(() -> Component.literal("Schematic loaded at " + origin), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to load schematic: " + e.getMessage()));
            NeoBlockMod.LOGGER.error("Failed to load schematic", e);
        }
    }

    public static NeoSchematic fromNBT(CompoundTag tag, ServerLevel level) {
        Optional<BlockPos> origin = NbtUtils.readBlockPos(tag, "origin");
        List<NeoSchematic.BlockInfo> blocks = new ArrayList<>();
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);

        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;
            Optional<BlockPos> offset = NbtUtils.readBlockPos(blockTag, "pos");
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), blockTag.getCompound("state"));

            CompoundTag nbt = blockTag.contains("be") ? blockTag.getCompound("be") : null;
            blocks.add(new NeoSchematic.BlockInfo(offset.orElse(BlockPos.ZERO), state, nbt));
        }

        return new NeoSchematic(origin.orElse(NeoBlock.POS), blocks);
    }

    public record BlockInfo(BlockPos offset, BlockState state, @Nullable CompoundTag nbt) {}

    private final List<BlockInfo> blocks;
    private final BlockPos origin;

    public NeoSchematic(BlockPos origin, List<BlockInfo> blocks) {
        this.origin = origin;
        this.blocks = blocks;
    }

    public NeoSchematic(ServerLevel level, BlockPos pos1, BlockPos pos2, BlockPos center) {
        HolderLookup.Provider registries = level.registryAccess();

        BlockPos min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos max = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));

        this.blocks = new ArrayList<>();
        this.origin = center == null ? pos1 : center;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            CompoundTag nbt = null;

            BlockEntity block = level.getBlockEntity(pos);
            if (block != null) nbt = block.saveWithFullMetadata(registries);

            blocks.add(new BlockInfo(pos.subtract(origin), state, nbt));
        }
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("origin", NbtUtils.writeBlockPos(origin));

        ListTag blockList = new ListTag();
        for (BlockInfo info : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("pos", NbtUtils.writeBlockPos(info.offset()));
            blockTag.put("state", NbtUtils.writeBlockState(info.state()));
            if (info.nbt() != null) blockTag.put("be", info.nbt());
            blockList.add(blockTag);
        }

        tag.put("blocks", blockList);
        return tag;
    }

    public void place(ServerLevel level, BlockPos targetOrigin) {
        HolderLookup.Provider registries = level.registryAccess();

        for (BlockInfo info : blocks) {
            BlockPos pos = targetOrigin.offset(info.offset());
            level.setBlock(pos, info.state(), Block.UPDATE_ALL);

            if (info.nbt() != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    be.loadWithComponents(info.nbt(), registries);
                    be.setChanged();
                }
            }
        }
    }
}
