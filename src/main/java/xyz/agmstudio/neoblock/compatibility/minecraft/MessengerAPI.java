package xyz.agmstudio.neoblock.compatibility.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.util.*;

public class MessengerAPI {
    private static final HashMap<ServerLevel, List<MessageHolder>> messages = new HashMap<>();

    public static void sendMessage(String key, ServerLevel level, boolean action, Object... args) {
        sendMessage(Component.translatable(key, args), level, action);
    }

    public static void sendMessage(Component message, ServerLevel level, boolean action) {
        NeoBlockMod.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
    }

    public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, action);
    }

    public static void sendInstantMessage(Component message, Level level, boolean action) {
        NeoBlockMod.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);
    }

    public static void onPlayerJoin(ServerLevel level, Player player) {
        messages.getOrDefault(null, new ArrayList<>()).forEach(holder -> holder.send(player));
        messages.getOrDefault(level, new ArrayList<>()).forEach(holder -> holder.send(player));
    }

    static class MessageHolder {
        private final Set<Player> players = new HashSet<>();
        private final Component message;
        private final boolean action;

        protected MessageHolder(Component message, boolean action) {
            this.message = message;
            this.action = action;
        }

        public void send(Player player) {
            if (players.add(player)) player.displayClientMessage(message, action);
        }
    }
}
