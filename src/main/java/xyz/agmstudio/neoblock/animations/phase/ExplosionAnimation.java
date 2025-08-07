package xyz.agmstudio.neoblock.animations.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.util.ConfigUtil;

public class ExplosionAnimation extends UpgradePhaseAnimation {
    @ConfigUtil.ConfigField("at-start")
    private boolean activeOnUpgradeStart = false;
    @ConfigUtil.ConfigField("at-finish")
    private boolean activeOnUpgradeFinish = true;
    @ConfigUtil.ConfigField(min = 0)
    private float volume = 0.7f;

    public ExplosionAnimation() {
        super("explosion");
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

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                BlockManager.getBlockPos().getX() + 0.5,
                BlockManager.getBlockPos().getY() + 0.5,
                BlockManager.getBlockPos().getZ() + 0.5,
                1, 0, 0, 0, 1);

        level.playSound(null, BlockManager.getBlockPos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, volume, 0.4f);
    }
}
