package xyz.agmstudio.neoblock.animations.phase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

public class ExplosionAnimation extends UpgradePhaseAnimation {
    @AnimationConfig("at-start")
    private boolean activeOnUpgradeStart = false;
    @AnimationConfig("at-finish")
    private boolean activeOnUpgradeFinish = true;
    @AnimationConfig private float volume = 0.7f;

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
        this.volume = Math.max(0, volume);

        this.enabled = activeOnUpgradeStart || activeOnUpgradeFinish;
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                NeoBlock.POS.getX() + 0.5,
                NeoBlock.POS.getY() + 0.5,
                NeoBlock.POS.getZ() + 0.5,
                1, 0, 0, 0, 1);

        level.playSound(null, NeoBlock.POS, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, volume, 0.4f);
    }
}
