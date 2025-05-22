package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ConfigUtil;

public class BreakingAnimation extends UpgradeProgressAnimation {
    @ConfigUtil.ConfigField(min = 0)
    private float volume = 0.7f;

    public BreakingAnimation() {
        super("breaking");
    }

    @Override public void animate(ServerLevel level, LevelAccessor access) {
        level.levelEvent(2001, WorldData.POS, Block.getId(Blocks.BEDROCK.defaultBlockState()));
        level.playSound(null, WorldData.POS, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, volume, 1.0f);
    }
}
