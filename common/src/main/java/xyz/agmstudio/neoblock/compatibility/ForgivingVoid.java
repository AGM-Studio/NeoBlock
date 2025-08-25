package xyz.agmstudio.neoblock.compatibility;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ForgivingVoid {
    public static boolean handleVoid(ServerLevel level, Entity entity) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        entity.teleportTo(0, y + 0.5, 0);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;

        if (entity instanceof LivingEntity living)
            living.hurt(living.damageSources().fall(), living.getMaxHealth() - 1.0F);

        return true;
    }
}
