package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

public class NeoBlockPos extends BlockPos {
    private final ServerLevel level;

    protected NeoBlockPos() {
        this(WorldManager.getWorldData().getBlockPos());
    }

    protected NeoBlockPos(BlockPos pos) {
        this(pos, WorldManager.getWorldData().getDimension());
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
        ServerLevel level = WorldManager.getWorldData().getDimension();
        BlockPos pos = WorldManager.getWorldData().getBlockPos();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return new NeoBlockPos(pos.getX(), y, pos.getZ(), level);
    }
    public static NeoBlockPos get() {
        return new NeoBlockPos();
    }
    public static Vec3 getCorner() {
        BlockPos pos = WorldManager.getWorldData().getBlockPos();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public ServerLevel getLevel() {
        return level;
    }
    public void teleportTo(Entity entity) {
        MinecraftUtil.teleportEntity(entity, level, getX() + 0.5, getY() + 0.5, getZ() + 0.5, 0, 0);
    }
    public void teleportTo(Entity entity, Vec3 offset) {
        MinecraftUtil.teleportEntity(entity, level, getX() + offset.x, getY() + offset.y, getZ() + offset.z, 0, 0);
    }
    public void teleportTo(Entity entity, Vec3 offset, int ry, int rx) {
        MinecraftUtil.teleportEntity(entity, level, getX() + offset.x, getY() + offset.y, getZ() + offset.z, ry, rx);
    }
}
