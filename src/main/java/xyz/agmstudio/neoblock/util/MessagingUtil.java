package xyz.agmstudio.neoblock.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessagingUtil {
    private final static HashMap<Level, List<MessageHolder>> messages = new HashMap<>();

    public static void sendMessage(String key, Level level, Object... args) {
        sendMessage(Component.translatable(key, args), level, false);
    }
    public static void sendMessage(String key, Level level, boolean action, Object... args) {
        sendMessage(Component.translatable(key, args), level, action);
    }
    public static void sendMessage(Component message, Level level, boolean action) {
        NeoBlockMod.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player: level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
    }
    public static void sendInstantMessage(String key, Level level, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, false);
    }
    public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, action);
    }
    public static void sendInstantMessage(Component message, Level level, boolean action) {
        NeoBlockMod.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player: level.players()) holder.send(player);
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player)
            messages.getOrDefault(event.getLevel(), new ArrayList<>())
                    .forEach(holder -> holder.send(player));
    }

    static class MessageHolder {
        private final List<Player> players = new ArrayList<>();
        private final Component message;
        private final boolean action;

        protected MessageHolder(Component message, boolean action) {
            this.message = message;
            this.action = action;
        }

        public void send(Player player) {
            if (players.contains(player)) return;
            player.displayClientMessage(message, action);
            players.add(player);
        }
    }
}
