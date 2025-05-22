package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ConfigUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class SparkleAnimation extends UpgradeProgressAnimation {
    @ConfigUtil.ConfigField(min = 1)
    private int length = 10;
    @ConfigUtil.ConfigField(min = 1)
    private int factor = 3;
    @ConfigUtil.ConfigField(min = 1)
    private int count = 5;

    private final List<Integer> animations = new ArrayList<>();

    public SparkleAnimation() {
        super("sparkle");
    }

    @Override
    public void processConfig() {
        length = Math.min(length, interval);
    }

    @Override
    public void animate(ServerLevel level, LevelAccessor access) {
        animations.add(0);
    }

    @Override
    public void tick(ServerLevel level, LevelAccessor access) {
        Iterator<Integer> iterator = new ArrayList<>(animations).iterator();
        animations.clear();

        while (iterator.hasNext()) {
            int tick = iterator.next();
            if (tick % factor == 0) {
                for (int i = 0; i < 1 + (length / 2) - Math.abs(tick - (length / 2)); i++) {
                    Vec3 glowPos = Vec3.atCenterOf(WorldData.POS).add(
                            (level.getRandom().nextDouble() - 0.5) * 1.2,
                            level.getRandom().nextDouble() * 1.5,
                            (level.getRandom().nextDouble() - 0.5) * 1.2
                    );
                    level.sendParticles(ParticleTypes.GLOW, glowPos.x, glowPos.y, glowPos.z, 1, 0, 0, 0, 0.05);
                }
            }

            // Continue the animation until reaching the defined length
            if (++tick < length) animations.add(tick);
        }
    }
}