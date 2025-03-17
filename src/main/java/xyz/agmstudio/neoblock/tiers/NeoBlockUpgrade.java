package xyz.agmstudio.neoblock.tiers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.animations.*;

import java.util.ArrayList;
import java.util.List;

public class NeoBlockUpgrade {
    protected int UPGRADE_TICKS = 0;
    protected int UPGRADE_GOAL = 0;
    private final ServerBossEvent UPGRADE_BAR;

    private final List<UpgradeAnimation> animations = new ArrayList<>();
    private final List<Animation> finishAnimations = new ArrayList<>();
    private final List<Animation> startAnimations = new ArrayList<>();

    public NeoBlockUpgrade() {
        UPGRADE_BAR = Config.AnimateProgressbar.get() ? new ServerBossEvent(
                Component.translatable("bossbar.neoblock.upgrade_bar", ""),
                BossEvent.BossBarColor.byName(Config.AnimateProgressbarColor.get().toLowerCase()),
                BossEvent.BossBarOverlay.PROGRESS
        ) : null;
        if (Config.AnimateBlockBreaking.get()) animations.add(new UpgradeBreaking());
        if (Config.AnimateBlockSparkle.get()) animations.add(new UpgradeSparkle());
        if (Config.AnimateBlockSpiral.get()) animations.add(new UpgradeSpiral());

        if (Config.AnimateUpgradeFuse.get()) startAnimations.add(new FuseAnimation());

        if (Config.AnimateUpgradeExplosion.get()) finishAnimations.add(new ExplosionAnimation());
    }


    private static @NotNull String formatTicks(int ticks) {
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder timeString = new StringBuilder();
        if (hours > 0) timeString.append(hours).append("h ");
        if (minutes > 0 || hours > 0) timeString.append(minutes).append("m ");
        timeString.append(seconds).append("s");

        return timeString.toString().trim();
    }

    public boolean isOnUpgrade() {
        return UPGRADE_GOAL > 0;
    }

    public void tick(ServerLevel level, LevelAccessor access) {
        for (UpgradeAnimation animation: animations) animation.tick(level, access);
        if (!isOnUpgrade()) return;
        if (++UPGRADE_TICKS >= UPGRADE_GOAL) finishUpgrade(level, access);
        else {
            UPGRADE_BAR.setProgress((float) UPGRADE_TICKS / UPGRADE_GOAL);
            UPGRADE_BAR.setName(Component.translatable("bossbar.neoblock.upgrade_bar", formatTicks(UPGRADE_GOAL - UPGRADE_TICKS)));

            for (UpgradeAnimation animation: animations) animation.upgradeTick(level, access, UPGRADE_TICKS);
        }
        NeoBlock.DATA.setDirty();
    }

    protected void finishUpgrade(ServerLevel level, LevelAccessor access) {
        if (NeoBlock.DATA == null) return;
        NeoTier tier = NeoBlock.DATA.getTier().next();
        if (tier != null && tier.isUnlocked()) {
            tier.onFinishUpgrade(level);
            NeoBlock.DATA.setTier(tier);
        }

        for (Animation animation: finishAnimations) animation.animate(level, access);

        NeoBlock.setNeoBlock(access, NeoBlock.getRandomBlock());

        UPGRADE_BAR.removeAllPlayers();
        UPGRADE_TICKS = 0;
        UPGRADE_GOAL = 0;
    }

    public void startUpgrade(ServerLevel level, LevelAccessor access, NeoTier tier) {
        if (tier == null || !tier.isUnlocked()) return;
        NeoBlock.setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());
        UPGRADE_GOAL += tier.UNLOCK_TIME;
        tier.onStartUpgrade(level);
        if (UPGRADE_GOAL == 0) finishUpgrade(level, access);
        else level.players().forEach(UPGRADE_BAR::addPlayer);

        for (Animation animation: startAnimations) animation.animate(level, access);
    }

    protected void configure(int goal, int tick) {
        UPGRADE_GOAL = goal;
        UPGRADE_TICKS = tick;
    }

    public void showTo(ServerPlayer player) {
        UPGRADE_BAR.addPlayer(player);
    }
}
