package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neocore.NeoMC;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeoFlowAnimation extends IdleAnimation {
    @ConfigField(min = 1)
    private int count = 1;
    @ConfigField(min = 0.01)
    private float speed = 0.05f;
    @ConfigField(value = "color-speed", min = 0, max = 1)
    private float hueSpeed = 1.0f;
    @ConfigField(value = "wait-for", min = 0)
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

    @Override public void tick(NeoBlock block) {
        if (tick++ > delay) animate(block);
    }

    @Override public void animate(NeoBlock block) {
        Vec3[] corners = getCorners(block);
        if (particles.isEmpty()) for (int i = 0; i < count; i++)
            particles.add(new AnimationParticle(block, corners[block.level.random.nextInt(corners.length)]));
        for (AnimationParticle particle: particles) {
            Vec3 next = particle.next(speed);
            Vector3f color = getRainbowColor();
            block.level.sendParticles(NeoMC.getDustParticle(color, 1.0f), next.x, next.y, next.z, 1, 0, 0, 0, 0.01);
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
        private final NeoBlock block;
        private Vec3 current;
        private Vec3 goal;
        private Vec3 last;
        private Vec3 direction;

        private AnimationParticle(NeoBlock block, Vec3 start) {
            this.block = block;
            this.current = start;
            this.last = start;
            updateGoal(start);
        }
        private Vec3 next(float speed) {
            if (current.distanceTo(goal) < speed) {
                current = goal;
                updateGoal(current);
            } else current = current.add(direction.scale(speed));
            return current;
        }

        private void updateGoal(Vec3 vec3) {
            last = goal;
            List<Vec3> options = Arrays.stream(getCorners(block)).filter(vec -> vec.distanceToSqr(vec3) == 1 && vec != this.last).toList();
            goal = options.get(block.level.random.nextInt(options.size()));
            direction = goal.subtract(current).normalize();
        }
    }
}