package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.util.ConfigUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpiralAnimation extends UpgradeProgressAnimation {
    @ConfigUtil.ConfigField(min = 1)
    private int length = 50;
    @ConfigUtil.ConfigField(min = 1)
    private int count = 5;

    private final List<Integer> animations = new ArrayList<>();

    public SpiralAnimation() {
        super("spiral");
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        animations.add(0);
    }

    @Override public void tick(ServerLevel level, LevelAccessor access) {
        Iterator<Integer> iterator = new ArrayList<>(animations).iterator();
        animations.clear();

        while (iterator.hasNext()) {
            int tick = iterator.next();
            double progress = (double) tick / length;

            for (int i = 0; i < count; i++) {
                double angle = (tick + (i * (360.0 / count))) * (Math.PI / 10);
                double radius = 0.5;
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;
                double yOffset = progress * 1.2;

                Vec3 particlePos = Vec3.atCenterOf(BlockManager.getBlockPos()).add(xOffset, yOffset, zOffset);
                level.sendParticles(ParticleTypes.ENCHANT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0.02);
            }

            if (++tick < length) animations.add(tick);
        }
    }
}
