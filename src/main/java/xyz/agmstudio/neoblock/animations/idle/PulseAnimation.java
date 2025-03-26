package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.util.HashSet;
import java.util.Iterator;

public class PulseAnimation extends IdleAnimation {
    @AnimationConfig private int interval = 100;
    @AnimationConfig("wait-for")
    private int delay = 200;

    private long tick = 0;

    public PulseAnimation() {
        super("pulse");
    }

    @Override public void resetTick() {
        tick = 0;
    }

    @Override public void processConfig() {
        interval = Math.max(interval, 5);
        delay = Math.max(delay, 0);
    }

    @Override public void tick(ServerLevel level, LevelAccessor access) {
        if (tick++ % interval != 0 || tick < delay) return;
        NeoBlockMod.LOGGER.info("PulseAnimation tick: " + tick);
        animate(level, access);
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        for (HashSet<Vec3> edge: EDGES) {
            Iterator<Vec3> iter = edge.iterator();
            Vec3 from = iter.next();
            Vec3 step = iter.next().subtract(from).scale(0.1);
            for (int i = 0; i < 10; i++) {
                Vec3 pos = from.add(step.scale(i));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.01);
            }
        }
    }
}
