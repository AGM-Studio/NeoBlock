package xyz.agmstudio.neoblock.animations.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.neo.block.BlockManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FuseAnimation extends UpgradePhaseAnimation {
    @ConfigField("at-start")
    private boolean activeOnUpgradeStart = false;
    @ConfigField("at-finish")
    private boolean activeOnUpgradeFinish = true;
    @ConfigField(min = 0)
    private float volume = 1.5f;

    private final List<Integer> animations = new ArrayList<>();

    public FuseAnimation() {
        super("fuse");
    }

    @Override public boolean isActiveOnUpgradeStart() {
        return activeOnUpgradeStart;
    }
    @Override public boolean isActiveOnUpgradeFinish() {
        return activeOnUpgradeFinish;
    }

    @Override public void processConfig() {
        this.enabled = activeOnUpgradeStart || activeOnUpgradeFinish;
    }

    @Override public void animate(ServerLevel level) {
        animations.add(0);
    }

    @Override public void tick(ServerLevel level) {
        Iterator<Integer> iterator = new ArrayList<>(animations).iterator();
        animations.clear();

        while (iterator.hasNext()) {
            int tick = iterator.next();
            if (tick == 0) level.playSound(null, BlockManager.getBlockPos(), SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, volume, 1.0f);

            for (int i = 0; i < 8; i++) {
                Vec3 particlePos = Vec3.atCenterOf(BlockManager.getBlockPos()).add(
                        (level.getRandom().nextDouble() - 0.5) * 1.5,
                        0.55,
                        (level.getRandom().nextDouble() - 0.5) * 1.5
                );
                level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 1, 0, 0, 0, 0.02);
            }
            if (tick % 5 == 0) {
                Vec3 sparkPos = Vec3.atCenterOf(BlockManager.getBlockPos()).add(
                        (level.getRandom().nextDouble() - 0.5) * 0.7,
                        level.getRandom().nextDouble() * 1.1,
                        (level.getRandom().nextDouble() - 0.5) * 0.7
                );
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sparkPos.x, sparkPos.y, sparkPos.z, 1, 0, 0, 0, 0.05);
            }

            if (++tick < 50) animations.add(tick);
        }
    }
}
