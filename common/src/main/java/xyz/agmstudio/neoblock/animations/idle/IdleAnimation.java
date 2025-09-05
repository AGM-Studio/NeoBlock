package xyz.agmstudio.neoblock.animations.idle;

import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public abstract class IdleAnimation extends Animation {
    protected static Vec3[] getCorners() {
        return Stream.of(
                new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1), new Vec3(0, 0, 1),
                new Vec3(0, 1, 0), new Vec3(1, 1, 0), new Vec3(1, 1, 1), new Vec3(0, 1, 1)
        ).map(vec3 -> vec3.add(NeoBlockPos.getCorner())).toArray(Vec3[]::new);
    }

    protected static HashSet<HashSet<Vec3>> getEdges() {
        HashSet<HashSet<Vec3>> edges = new HashSet<>();
        Vec3[] corners = getCorners();
        for (Vec3 corner: corners) {
            List<Vec3> options = Arrays.stream(corners).filter(vec -> vec.distanceToSqr(corner) == 1).toList();
            for (Vec3 option: options) {
                HashSet<Vec3> edge = new HashSet<>();
                edge.add(option);
                edge.add(corner);
                edges.add(edge);
            }
        }

        return edges;
    }

    public IdleAnimation(String name) {
        super("idle", name);
    }

    @Override protected void onRegister() {}

    public abstract void resetTick();
}
