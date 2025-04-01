package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import xyz.agmstudio.neoblock.util.ConfigUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeoFlowAnimation extends IdleAnimation {
    @ConfigUtil.ConfigField(min = 1)
    private int count = 1;
    @ConfigUtil.ConfigField(min = 0.01)
    private float speed = 0.05f;
    @ConfigUtil.ConfigField(value = "color-speed", min = 0, max = 1)
    private float hueSpeed = 1.0f;
    @ConfigUtil.ConfigField(value = "wait-for", min = 0)
    private int delay = 200;
    
    private final List<AnimationParticle> particles = new ArrayList<>();
    private float hue = 0.0f;
    private long tick = 0;

    public NeoFlowAnimation() {
        super("neo-flow");
    }

    @Override public void resetTick() {
        tick = 0;
    }

    @Override public void tick(ServerLevel level, LevelAccessor access) {
        if (tick++ > delay) animate(level, access);
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        if (particles.isEmpty()) for (int i = 0; i < count; i++)
            particles.add(AnimationParticle.fromRandom(level.getRandom()));

        for (AnimationParticle particle: particles) {
            Vec3 next = particle.next(speed);
            Vector3f color = getRainbowColor();
            level.sendParticles(new DustParticleOptions(color, 1.0f), next.x, next.y, next.z, 1, 0, 0, 0, 0.01);
        }
    }

    private Vector3f getRainbowColor() {
        hue += hueSpeed / 20.0f;
        while (hue > 1.0f) hue -= 1.0f;

        int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        return new Vector3f(r, g, b);
    }

    private static class AnimationParticle {
        private static AnimationParticle fromRandom(RandomSource random) {
            return new AnimationParticle(random, CORNERS[random.nextInt(CORNERS.length)]);
        }

        private final RandomSource random;
        private Vec3 current;
        private Vec3 goal;
        private Vec3 last;
        private Vec3 direction;

        private AnimationParticle(RandomSource random, Vec3 start) {
            this.random = random;

            this.current = start;
            this.last = start;
            updateGoal(start);
        }
        private Vec3 next(float speed) {
            if (current.distanceTo(goal) < speed) {
                current = goal;
                updateGoal(current);
            } else current = current.add(direction.scale(speed));;
            return current;
        }

        private void updateGoal(Vec3 vec3) {
            List<Vec3> options = Arrays.stream(CORNERS).filter(vec -> vec.distanceToSqr(vec3) == 1 && vec != this.last).toList();
            last = goal;
            goal = options.get(random.nextInt(options.size()));
            direction = goal.subtract(current).normalize();
        }
    }
}