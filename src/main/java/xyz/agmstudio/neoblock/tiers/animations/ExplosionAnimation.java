package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

public class ExplosionAnimation extends Animation {
    private final float volume;

    public ExplosionAnimation() {
        super();
        this.volume = Math.min(0, Config.AnimateBlockBreakingVolume.get());
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
