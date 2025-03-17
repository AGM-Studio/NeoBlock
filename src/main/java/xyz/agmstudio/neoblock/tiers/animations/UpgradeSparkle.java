package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

import java.util.ArrayList;
import java.util.List;

public class UpgradeSparkle extends UpgradeAnimation {
    private final int interval;
    private final int length;
    private final int factor;

    private final List<Integer> animations = new ArrayList<>();

    public UpgradeSparkle() {
        super();
        this.interval = Config.AnimateBlockSpiralInterval.get();
        this.length = Math.clamp(Config.AnimateBlockSparkleLength.get(), 0, interval);
        this.factor = Math.min(1, Config.AnimateBlockSparkleFactor.get());
    }

    @Override public void upgradeTick(ServerLevel level, LevelAccessor access, int tick) {
        int ticks = tick % interval;
        if (ticks < length && ticks % factor == 0) {
            for (int i = 0; i < 1 + (length / 2) - Math.abs(ticks - (length / 2)); i++) {
                Vec3 glowPos = Vec3.atCenterOf(NeoBlock.POS).add(
                        (level.getRandom().nextDouble() - 0.5) * 1.2,
                        level.getRandom().nextDouble() * 1.5,
                        (level.getRandom().nextDouble() - 0.5) * 1.2
                );
                level.sendParticles(ParticleTypes.GLOW, glowPos.x, glowPos.y, glowPos.z, 1, 0, 0, 0, 0.05);
            }
        }
    }
}
