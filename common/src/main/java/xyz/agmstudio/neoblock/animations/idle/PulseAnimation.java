package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Iterator;

public class PulseAnimation extends IdleAnimation {
    @ConfigField(min = 5)
    private int interval = 100;
    @ConfigField(value = "wait-for", min = 0)
    private int delay = 200;

    private long tick = 0;

    public PulseAnimation() {
        super("pulse");
    }

    @Override public void resetTick() {
        tick = 0;
    }

    @Override public void tick(ServerLevel level) {
        if (tick++ % interval != 0 || tick < delay) return;
        animate(level);
    }

    @Override public void animate(ServerLevel level) {
        for (HashSet<Vec3> edge: getEdges()) {
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
