package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

public class UpgradeBreaking extends UpgradeAnimation {
    private final int interval;
    private final float volume;

    public UpgradeBreaking() {
        super();
        this.interval = Config.AnimateBlockBreakingInterval.get();
        this.volume = Math.clamp(Config.AnimateBlockBreakingVolume.get(), 0.0f, 1.0f);
    }

    @Override public void upgradeTick(ServerLevel level, LevelAccessor access, int tick) {
        if (tick % interval == 0) {
            level.levelEvent(2001, NeoBlock.POS, Block.getId(Blocks.BEDROCK.defaultBlockState()));
            level.playSound(null, NeoBlock.POS, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, volume, 1.0f);
        }
    }
}
