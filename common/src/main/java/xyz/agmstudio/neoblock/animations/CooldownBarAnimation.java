package xyz.agmstudio.neoblock.animations;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import xyz.agmstudio.neoblock.util.StringUtil;

public class CooldownBarAnimation extends Animation {
    @ConfigField
    private String color = "red";
    @ConfigField("show-time")
    private boolean dynamic = true;

    private final ServerBossEvent bar;

    public CooldownBarAnimation() {
        super("progressbar");
        bar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    }

    public void update(long ticks, long goal) {
        bar.setProgress((float) ticks / goal);
        MutableComponent name = dynamic ?
                Component.translatable("bossbar.neoblock.upgrade_bar", StringUtil.formatTicks(goal - ticks)) :
                Component.translatable("bossbar.neoblock.upgrade_bar_no_time");

        bar.setName(name);
    }

    public void addPlayerToBar(ServerPlayer player) {
        bar.addPlayer(player);
    }
    public void removeAllPlayers() {
        bar.removeAllPlayers();
    }

    @Override protected void onRegister() {
        bar.setColor(BossEvent.BossBarColor.byName(color));
    }
}
