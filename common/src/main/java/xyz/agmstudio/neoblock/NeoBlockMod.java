package xyz.agmstudio.neoblock;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.configs.TierConfig;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.world.WorldCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.schematics.Schematic;
import xyz.agmstudio.neocore.NeoMod;
import xyz.agmstudio.neocore.platform.IConfig;
import xyz.agmstudio.neocore.NeoMC;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;


public abstract class NeoBlockMod extends NeoMod {
    public static final String MOD_ID = "neoblock";
    public static final String MOD_NAME = "NeoBlock";

    private static NeoBlockMod instance;

    public static NeoBlockMod get() {
        return instance;
    }
    public static IRegistryHelper getRegistry() {
        return instance.registry;
    }
    public static IConfig getConfig() {
        return instance.getModConfig();
    }
    public static Logger getLogger() {
        return instance.getModLogger();
    }

    private final IRegistryHelper registry;
    protected NeoBlockMod(IRegistryHelper registry) {
        super(NeoBlockMod.MOD_ID, NeoBlockMod.MOD_NAME);
        this.registry = registry;

        NeoBlockMod.instance = this;

        // To make sure files & folders are created.
        WorldManager.reloadConfig();
        TierConfig.loadAllTierConfigs();
        // Schematic class will take care of it.
        boolean ignored = Schematic.folder.toFile().exists();

        if (NeoMC.isDevelopmentEnvironment()) {
            Configurator.setRootLevel(ERROR);
            Configurator.setLevel(getLogger().getName(), DEBUG);
            getLogger().debug("Enabling debug mode for neoblock (development environment)");
        }

        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(BlockManager::tick);
        NeoListener.registerTicker(WorldCooldown::tick);

        NeoMobSpec.load();
    }

    protected abstract <T extends SavedData> T captureSavedDataImpl(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator);
    protected abstract WorldManager instanceWorldDataImpl(ServerLevel level);

    public static <T extends SavedData> T captureSavedData(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator) {
	    return instance.captureSavedDataImpl(level, name, loader, creator);
    }
    public static WorldManager instanceWorldData(ServerLevel level) {
	    return instance.instanceWorldDataImpl(level);
    }

    private static final HashMap<ServerLevel, List<MessageHolder>> messages = new HashMap<>();
    public static void sendMessage(String key, ServerLevel level, boolean action, Object... args) {
        sendMessage(Component.translatable(key, args), level, action);
    }
    public static void sendMessage(Component message, ServerLevel level, boolean action) {
        getLogger().info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
    }
    public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, action);
    }
    public static void sendInstantMessage(Component message, Level level, boolean action) {
        getLogger().info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);
    }

    public static void onPlayerJoin(ServerLevel level, Player player) {
        messages.getOrDefault(null, new ArrayList<>()).forEach(holder -> holder.send(player));
        messages.getOrDefault(level, new ArrayList<>()).forEach(holder -> holder.send(player));
    }
    public static void warnPlayers(@NotNull LevelAccessor level, String message, Object... objects) {
        String formatted = MessageFormatter.arrayFormat(message, objects).getMessage();
        getLogger().warn(message, objects);
        if (level instanceof net.minecraft.world.level.Level server)
            sendInstantMessage(Component.translatable("message.neoblock.warning", formatted).withStyle(ChatFormatting.RED), server, false);
    }

    private static final class MessageHolder {
        private final Set<Player> players = new HashSet<>();
        private final Component message;
        private final boolean action;

        private MessageHolder(Component message, boolean action) {
            this.message = message;
            this.action = action;
        }

        public void send(Player player) {
            if (players.add(player)) player.displayClientMessage(message, action);
        }
    }
}