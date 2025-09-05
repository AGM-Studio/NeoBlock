package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.neo.block.BlockManager;

public class BreakingAnimation extends UpgradeProgressAnimation {
    @ConfigField(min = 0)
    private float volume = 0.7f;

    public BreakingAnimation() {
        super("breaking");
    }

    @Override public void animate(ServerLevel level) {
        level.levelEvent(2001, BlockManager.getBlockPos(), Block.getId(Blocks.BEDROCK.defaultBlockState()));
        level.playSound(null, BlockManager.getBlockPos(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, volume, 1.0f);
    }
}
