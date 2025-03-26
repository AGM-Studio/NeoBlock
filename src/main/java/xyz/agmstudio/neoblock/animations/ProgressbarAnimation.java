package xyz.agmstudio.neoblock.animations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;

public class ProgressbarAnimation extends Animation {
    @AnimationConfig private String color = "red";
    @AnimationConfig("show-time")
    private boolean dynamicName = true;

    private final ServerBossEvent bar;

    public ProgressbarAnimation() {
        super("animations.progressbar");
        bar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    }

    public void update(int ticks, int goal) {
        bar.setProgress((float) ticks / goal);
        bar.setName(Component.translatable("bossbar.neoblock.upgrade_bar", formatTicks(goal - ticks)));
    }

    public void addPlayer(ServerPlayer player) {
        bar.addPlayer(player);
    }
    public void removeAllPlayers() {
        bar.removeAllPlayers();
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

    @Override protected void onRegister() {
        bar.setColor(BossEvent.BossBarColor.byName(color));
    }
    @Override protected void processConfig() {}
}
