package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;

public class BreakingAnimation extends CooldownProgressAnimation {
    @ConfigField(min = 0)
    private float volume = 0.7f;

    public BreakingAnimation() {
        super("breaking");
    }

    @Override public void animate(NeoBlock block) {
        block.level.levelEvent(2001, block.getBlockPos(), Block.getId(Blocks.BEDROCK.defaultBlockState()));
        block.level.playSound(null, block.getBlockPos(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, volume, 1.0f);
    }
}