package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.Set;

public class NeoBlockPos extends BlockPos {
    private final ServerLevel level;

    protected NeoBlockPos() {
        this(WorldData.getWorldStatus().getBlockPos());
    }

    protected NeoBlockPos(BlockPos pos) {
        this(pos, WorldData.getWorldStatus().getDimension());
    }

    protected NeoBlockPos(BlockPos pos, ServerLevel level) {
        super(pos);
        this.level = level;
    }

    protected NeoBlockPos(int x, int y, int z, ServerLevel level) {
        super(x, y, z);
        this.level = level;
    }

    public static NeoBlockPos safeBlock() {
        ServerLevel level = WorldData.getWorldStatus().getDimension();
        BlockPos pos = WorldData.getWorldStatus().getBlockPos();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return new NeoBlockPos(pos.getX(), y, pos.getZ(), level);
    }
    public static NeoBlockPos get() {
        return new NeoBlockPos();
    }

    public ServerLevel getLevel() {
        return level;
    }
    public void teleportTo(Entity entity) {
        entity.teleportTo(level, getX() + 0.5, getY() + 0.5, getZ() + 0.5, Set.of(), 0, 0);
    }
    public void teleportTo(Entity entity, Vec3 offset) {
        entity.teleportTo(level, getX() + offset.x, getY() + offset.y, getZ() + offset.z, Set.of(), 0, 0);
    }
    public void teleportTo(Entity entity, Vec3 offset, int ry, int rx) {
        entity.teleportTo(level, getX() + offset.x, getY() + offset.y, getZ() + offset.z, Set.of(), ry, rx);
    }
}
