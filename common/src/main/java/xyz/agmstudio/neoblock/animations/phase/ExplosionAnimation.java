package xyz.agmstudio.neoblock.animations.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;

public class ExplosionAnimation extends CooldownPhaseAnimation {
    @ConfigField("at-start")
    private boolean activeOnUpgradeStart = false;
    @ConfigField("at-finish")
    private boolean activeOnUpgradeFinish = true;
    @ConfigField(min = 0)
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

    @Override public void animate(NeoBlock block) {
        block.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                block.getBlockPos().getX() + 0.5,
                block.getBlockPos().getY() + 0.5,
                block.getBlockPos().getZ() + 0.5,
                1, 0, 0, 0, 1);

        block.level.playSound(null, block.getBlockPos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, volume, 0.4f);
    }
}