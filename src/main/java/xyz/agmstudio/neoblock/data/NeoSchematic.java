package xyz.agmstudio.neoblock.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoSchematic {
    public static final Path folder = MinecraftUtil.CONFIG_DIR.resolve("schematics");
    static {
        if (folder.toFile().mkdirs()) NeoBlockMod.LOGGER.debug("Created {}", folder);
    }

    public static @Nullable Path saveSchematic(ServerLevel level, BlockPos pos1, BlockPos pos2, @Nullable BlockPos center, @Nullable String name) {
        CompoundTag nbt = new NeoSchematic(level, pos1, pos2, center).toNBT();

        try {
            if (name == null) {
                int i = 0;
                do name = "NeoBlockSchematic_" + i++ + ".nbt"; while (!folder.resolve(name).toFile().exists());
            } else if (!name.endsWith(".nbt")) name = name + ".nbt";

            Path file = folder.resolve(name);
            MinecraftUtil.NBT.IO.write(file, nbt);
            return file;
        } catch (IOException e) {
            NeoBlockMod.LOGGER.error("Failed to save schematic", e);
            return null;
        }
    }
    public static int loadSchematic(ServerLevel level, BlockPos origin, @Nullable String name) {
        try {
            if (name == null) {
                File[] files = folder.toFile().listFiles((file) -> file.getName().endsWith(".nbt"));
                String file = files != null && files.length > 0 ? files[0].getName() : "NeoBlockSchematic_0.nbt";
            } else if (!name.endsWith(".nbt")) {
                name = name + ".nbt";
            }

            Path file = name != null ? folder.resolve(name) : null;
            if (file == null || !Files.exists(file)) return 0;

            CompoundTag tag = MinecraftUtil.NBT.IO.read(file);
            NeoSchematic data = NeoSchematic.fromNBT(tag, level);
            data.place(level, origin);
            return 1;
        } catch (Exception e) {
            NeoBlockMod.LOGGER.error("Failed to load schematic", e);
            return -1;
        }
    }

    public static NeoSchematic fromNBT(CompoundTag tag, ServerLevel level) {
        BlockPos origin = MinecraftUtil.NBT.readBlockPos(tag, "origin", NeoBlock.POS);
        List<NeoSchematic.BlockInfo> blocks = new ArrayList<>();
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);

        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;
            BlockPos offset = MinecraftUtil.NBT.readBlockPos(blockTag, "pos", BlockPos.ZERO);
            BlockState state = MinecraftUtil.NBT.readBlockState(tag, "state", level);
            CompoundTag nbt = blockTag.contains("be") ? blockTag.getCompound("be") : null;
            blocks.add(new NeoSchematic.BlockInfo(offset, state, nbt));
        }

        return new NeoSchematic(origin, blocks);
    }

    public record BlockInfo(BlockPos offset, BlockState state, @Nullable CompoundTag nbt) {}

    private final List<BlockInfo> blocks;
    private final BlockPos origin;

    public NeoSchematic(BlockPos origin, List<BlockInfo> blocks) {
        this.origin = origin;
        this.blocks = blocks;
    }

    public NeoSchematic(ServerLevel level, BlockPos pos1, BlockPos pos2, BlockPos center) {
        BlockPos min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos max = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));

        this.blocks = new ArrayList<>();
        this.origin = center == null ? pos1 : center;

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            BlockEntity block = level.getBlockEntity(pos);
            CompoundTag nbt = MinecraftUtil.NBT.getBlockEntity(block, level);

            blocks.add(new BlockInfo(pos.subtract(origin), state, nbt));
        }
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("origin", MinecraftUtil.NBT.writeBlockPos(origin));

        ListTag blockList = new ListTag();
        for (BlockInfo info : blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("pos", MinecraftUtil.NBT.writeBlockPos(info.offset()));
            blockTag.put("state", MinecraftUtil.NBT.writeBlockState(info.state()));
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
            BlockEntity be = level.getBlockEntity(pos);
            MinecraftUtil.NBT.loadBlockEntity(be, info.nbt, level);
        }
    }
}
