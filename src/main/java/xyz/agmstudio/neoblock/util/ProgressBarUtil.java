package xyz.agmstudio.neoblock.util;


import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.BossEvent;

public class ProgressBarUtil {
    private static final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Progress Bar"), // Title
            BossEvent.BossBarColor.GREEN,                // Color of the bar
            BossEvent.BossBarOverlay.PROGRESS            // Style of the boss bar
    );

    private static float progress = 0f; // The value that changes gradually

    // Call this to add a player to see the boss ar
    public static void addPlayer(ServerPlayer player) {
        bossBar.addPlayer(player);
    }

    // Call this to remove a player when they leave
    public static void removePlayer(ServerPlayer player) {
        bossBar.removePlayer(player);
    }

    // Call this to update the progress dynamically
    public static void updateProgress(float value) {
        progress = Math.max(0, Math.min(value, 1)); // Ensure between 0 and 1
        bossBar.setProgress(progress);
    }

    // Auto-add players when they join
    public static void addAllPlayers(MinecraftServer server) {
        PlayerList playerList = server.getPlayerList();
        for (ServerPlayer player : playerList.getPlayers()) {
            bossBar.addPlayer(player);
        }
    }
}
