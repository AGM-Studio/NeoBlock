package xyz.agmstudio.neoblock.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
import xyz.agmstudio.neoblock.NeoBlock;

import java.util.*;

public class MessengerUtil {
    private static final HashMap<ServerLevel, List<MessageHolder>> messages = new HashMap<>();

    public static void sendMessage(String key, ServerLevel level, boolean action, Object... args) {
        sendMessage(Component.translatable(key, args), level, action);
    }

    public static void sendMessage(Component message, ServerLevel level, boolean action) {
        NeoBlock.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
    }

    public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, action);
    }

    public static void sendInstantMessage(Component message, Level level, boolean action) {
        NeoBlock.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);
    }

    public static void onPlayerJoin(ServerLevel level, Player player) {
        messages.getOrDefault(null, new ArrayList<>()).forEach(holder -> holder.send(player));
        messages.getOrDefault(level, new ArrayList<>()).forEach(holder -> holder.send(player));
    }

    public static void warnPlayers(@NotNull LevelAccessor level, String message, Object... objects) {
        String formatted = MessageFormatter.arrayFormat(message, objects).getMessage();
        NeoBlock.LOGGER.warn(message, objects);
        if (level instanceof Level server)
            sendInstantMessage(Component.translatable("message.neoblock.warn", formatted).withStyle(ChatFormatting.RED), server, false);
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
