package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.block.BlockManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public abstract class IdleAnimation extends Animation {
    protected static final Vec3[] CORNERS = Stream.of(
            new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1), new Vec3(0, 0, 1),
            new Vec3(0, 1, 0), new Vec3(1, 1, 0), new Vec3(1, 1, 1), new Vec3(0, 1, 1)
    ).map(vec3 -> vec3.add(BlockManager.POS_CORNER)).toArray(Vec3[]::new);
    protected static final HashSet<HashSet<Vec3>> EDGES = new HashSet<>();
    static {
        for (Vec3 corner: CORNERS) {
            List<Vec3> options = Arrays.stream(CORNERS).filter(vec -> vec.distanceToSqr(corner) == 1).toList();
            for (Vec3 option: options) {
                HashSet<Vec3> edge = new HashSet<>();
                edge.add(option);
                edge.add(corner);
                EDGES.add(edge);
            }
        }
    }

    public IdleAnimation(String name) {
        super("idle", name);
    }

    @Override protected void onRegister() {}

    public abstract void resetTick();

    private static final HashSet<Class<? extends IdleAnimation>> animations = new HashSet<>();
    public static void addAnimation(Class<? extends IdleAnimation> clazz) {
        if (!Animation.canRegisterNewAnimations()) return;
        animations.add(clazz);
    }
    public static @NotNull HashSet<IdleAnimation> getAnimations() {
        HashSet<IdleAnimation> animations = new HashSet<>();
        for (Class<? extends IdleAnimation> animation: IdleAnimation.animations) {
            try {
                animations.add(animation.getConstructor().newInstance());
            } catch (Exception e) {
                NeoBlockMod.LOGGER.error("Error while instantiating {}", animation, e);
            }
        }
        return animations;
    }
}
