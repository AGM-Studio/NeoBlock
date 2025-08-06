package xyz.agmstudio.neoblock.animations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

public class ProgressbarAnimation extends Animation {
    @ConfigUtil.ConfigField
    private String color = "red";
    @ConfigUtil.ConfigField("show-time")
    private boolean dynamicName = true;

    private final ServerBossEvent bar;

    public ProgressbarAnimation() {
        super("animations.progressbar");
        bar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    }

    public void update(long ticks, long goal) {
        bar.setProgress((float) ticks / goal);
        bar.setName(Component.translatable("bossbar.neoblock.upgrade_bar", StringUtil.formatTicks(goal - ticks)));
    }

    public void addPlayer(ServerPlayer player) {
        bar.addPlayer(player);
    }
    public void removeAllPlayers() {
        bar.removeAllPlayers();
    }

    @Override protected void onRegister() {
        bar.setColor(BossEvent.BossBarColor.byName(color));
    }
}
