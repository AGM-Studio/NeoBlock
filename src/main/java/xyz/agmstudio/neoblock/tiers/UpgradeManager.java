package xyz.agmstudio.neoblock.tiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.tiers.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.tiers.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.UpgradeProgressAnimation;

import java.util.HashSet;

public class UpgradeManager {
    private static final HashSet<UpgradeProgressAnimation> progressAnimations = new HashSet<>();
    private static final HashSet<UpgradePhaseAnimation> phaseAnimations = new HashSet<>();
    private static ProgressbarAnimation progressbar = null;

    public static void clearProgressAnimations() {
        progressAnimations.clear();
    }

    public static void addProgressAnimation(UpgradeProgressAnimation animation) {
        progressAnimations.add(animation);
    }

    public static void clearPhaseAnimations() {
        phaseAnimations.clear();
    }

    public static void addPhaseAnimation(UpgradePhaseAnimation animation) {
        phaseAnimations.add(animation);
    }

    protected int UPGRADE_TICKS = 0;
    protected int UPGRADE_GOAL = 0;

    protected UpgradeManager() {
        progressbar = new ProgressbarAnimation();
        if (!progressbar.isEnabled()) progressbar = null;
    }

    public boolean isOnUpgrade() {
        return UPGRADE_GOAL > 0;
    }

    public void tick(ServerLevel level, LevelAccessor access) {
        if (!isOnUpgrade()) return;
        for (UpgradeProgressAnimation animation : progressAnimations) animation.tick(level, access);
        if (++UPGRADE_TICKS >= UPGRADE_GOAL) finishUpgrade(level, access);
        else {
            if (progressbar != null) progressbar.update(UPGRADE_TICKS, UPGRADE_GOAL);
            for (UpgradeProgressAnimation animation : progressAnimations)
                animation.upgradeTick(level, access, UPGRADE_TICKS);
        }
        WorldData.getInstance().setDirty();
    }

    public void finishUpgrade(ServerLevel level, LevelAccessor access) {
        // todo fix this
        // NeoTier tier = NeoBlock.DATA.getTier().next();
        // if (tier != null && tier.isUnlocked()) {
        //     tier.onFinishUpgrade(level);
        //     NeoBlock.DATA.setTier(tier);
        // }

        if (progressbar != null) progressbar.removeAllPlayers();
        for (UpgradePhaseAnimation animation : phaseAnimations)
            if (animation.isActiveOnUpgradeFinish()) animation.animate(level, access);

        NeoBlock.setNeoBlock(access, NeoBlock.getRandomBlock());

        UPGRADE_TICKS = 0;
        UPGRADE_GOAL = 0;
    }

    public void startUpgrade(ServerLevel level, LevelAccessor access, NeoTier tier) {
        if (tier == null || !tier.isUnlocked()) return;
        NeoBlock.setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());
        UPGRADE_GOAL += tier.UNLOCK_TIME;
        tier.onStartUpgrade(level);
        if (UPGRADE_GOAL == 0) finishUpgrade(level, access);
        else if (progressbar != null) level.players().forEach(progressbar::addPlayer);

        for (UpgradePhaseAnimation animation : phaseAnimations)
            if (animation.isActiveOnUpgradeStart()) animation.animate(level, access);
    }

    protected void configure(int goal, int tick) {
        UPGRADE_GOAL = goal;
        UPGRADE_TICKS = tick;
    }

    public void addPlayer(ServerPlayer player) {
        if (progressbar != null) progressbar.addPlayer(player);
    }
}
