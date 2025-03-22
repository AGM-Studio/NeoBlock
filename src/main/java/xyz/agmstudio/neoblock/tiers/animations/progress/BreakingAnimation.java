package xyz.agmstudio.neoblock.tiers.animations.progress;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

public class BreakingAnimation extends UpgradeProgressAnimation {
    @AnimationConfig private float volume = 0.7f;

    public BreakingAnimation() {
        super("breaking");
    }

    @Override
    public void processConfig() {
        interval = Math.max(interval, 5);
        volume = Math.max(volume, 0.0f);
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        level.levelEvent(2001, NeoBlock.POS, Block.getId(Blocks.BEDROCK.defaultBlockState()));
        level.playSound(null, NeoBlock.POS, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, volume, 1.0f);
    }
}
