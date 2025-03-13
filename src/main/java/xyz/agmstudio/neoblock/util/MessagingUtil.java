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
        sendMessage(Component.translatable(key, args), level);
    }
    public static void sendMessage(Component message, Level level) {
        NeoBlockMod.LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message);
        for (Player player: level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
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

        protected MessageHolder(Component message) {
            this.message = message;
        }

        public void send(Player player) {
            if (players.contains(player)) return;
            player.displayClientMessage(message, false);
            players.add(player);
        }
    }
}
