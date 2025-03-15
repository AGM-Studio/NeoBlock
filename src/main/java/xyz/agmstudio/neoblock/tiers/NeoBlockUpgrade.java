package xyz.agmstudio.neoblock.tiers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class NeoBlockUpgrade {
    protected int UPGRADE_TICKS = 0;
    protected int UPGRADE_GOAL = 0;
    protected int UPGRADE_TO = -1;
    private final ServerBossEvent UPGRADE_BAR = new ServerBossEvent(
            Component.translatable("bossbar.neoblock.upgrade_bar", ""),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );

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
        if (isOnUpgrade()) {
            if (++UPGRADE_TICKS >= UPGRADE_GOAL) finishUpgrade(level, access);
            else {
                UPGRADE_BAR.setProgress((float) UPGRADE_TICKS / UPGRADE_GOAL);
                UPGRADE_BAR.setName(Component.translatable("message.neoblock.upgrade_bar", formatTicks(UPGRADE_GOAL - UPGRADE_TICKS)));
            }
        }
    }

    protected void finishUpgrade(ServerLevel level, LevelAccessor access) {
        NeoTier tier = NeoBlock.TIERS.get(UPGRADE_TO);
        NeoBlock.regenerateNeoBlock(level, access);
        tier.onGettingUnlocked(level);

        UPGRADE_BAR.removeAllPlayers();
        UPGRADE_TICKS = 0;
        UPGRADE_GOAL = 0;
        UPGRADE_TO = -1;
    }

    public void startUpgrade(ServerLevel level, LevelAccessor access, NeoTier tier) {
        UPGRADE_GOAL += tier.UNLOCK_TIME;
        UPGRADE_TO = tier.TIER;
        if (UPGRADE_GOAL == 0) finishUpgrade(level, access);
        else {
            level.players().forEach(UPGRADE_BAR::addPlayer);
            access.setBlock(NeoBlock.POS, Blocks.BEDROCK.defaultBlockState(), 3);
        }
    }
}
