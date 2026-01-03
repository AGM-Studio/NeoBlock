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
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.world.WorldCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.platform.INBTHelper;
import xyz.agmstudio.neoblock.platform.IRegistryHelper;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;


public abstract class NeoBlock {
    public static final String MOD_ID = "neoblock";
    public static final String MOD_NAME = "NeoBlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static final HashMap<ServerLevel, List<MessageHolder>> messages = new HashMap<>();

    public static Path CONFIG_FOLDER;

    public static final INBTHelper NBT_HELPER = loadService(INBTHelper.class);
    public static final IRegistryHelper REGISTRY = loadService(IRegistryHelper.class);

    private static NeoBlock instance;
    private static IConfig config;

    public static NeoBlock getInstance() {
        return instance;
    }
    public static IConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        NeoBlock.config = IConfig.getConfig(CONFIG_FOLDER, "config.toml");
    }

    protected NeoBlock(String name) {
        assert MOD_NAME.equals(name);

        NeoBlock.instance = this;
        NeoBlock.CONFIG_FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID);
        NeoBlock.config = IConfig.getConfig(CONFIG_FOLDER, "config.toml");

        // To make sure files & folders are created.
        WorldManager.reloadConfig();
        ResourceUtil.loadAllTierConfigs();
        // Schematic class will take care of it.
        boolean ignored = Schematic.folder.toFile().exists();

        if (NeoBlock.isDevelopmentEnvironment()) {
            Configurator.setRootLevel(ERROR);
            Configurator.setLevel(LOGGER.getName(), DEBUG);
            LOGGER.debug("Enabling debug mode for neoblock (development environment)");
        }

        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(BlockManager::tick);
        NeoListener.registerTicker(WorldCooldown::tick);

        NeoMobSpec.load();
    }

    public static <T> T loadService(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    protected abstract String getPlatformNameImpl();
    protected abstract boolean isModLoadedImpl(String modId);
    protected abstract boolean isDevelopmentEnvironmentImpl();
    protected abstract Path getConfigFolderImpl();
    protected abstract <T extends SavedData> T captureSavedDataImpl(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator);
    protected abstract WorldManager instanceWorldDataImpl(ServerLevel level);

    public static String getPlatformName() {
        return instance.getPlatformNameImpl();
    }
    public static boolean isModLoaded(String modId) {
	    return instance.isModLoadedImpl(modId);
    }
    public static boolean isDevelopmentEnvironment() {
	    return instance.isDevelopmentEnvironmentImpl();
    }
    public static Path getConfigFolder() {
	    return instance.getConfigFolderImpl();
    }
    public static <T extends SavedData> T captureSavedData(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator) {
	    return instance.captureSavedDataImpl(level, name, loader, creator);
    }
    public static WorldManager instanceWorldData(ServerLevel level) {
	    return instance.instanceWorldDataImpl(level);
    }

    public static void sendMessage(String key, ServerLevel level, boolean action, Object... args) {
        sendMessage(Component.translatable(key, args), level, action);
    }
    public static void sendMessage(Component message, ServerLevel level, boolean action) {
        LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);

        messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
    }
    public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
        sendInstantMessage(Component.translatable(key, args), level, action);
    }
    public static void sendInstantMessage(Component message, Level level, boolean action) {
        LOGGER.info(message.getString());

        MessageHolder holder = new MessageHolder(message, action);
        for (Player player : level.players()) holder.send(player);
    }

    public static void onPlayerJoin(ServerLevel level, Player player) {
        messages.getOrDefault(null, new ArrayList<>()).forEach(holder -> holder.send(player));
        messages.getOrDefault(level, new ArrayList<>()).forEach(holder -> holder.send(player));
    }
    public static void warnPlayers(@NotNull LevelAccessor level, String message, Object... objects) {
        String formatted = MessageFormatter.arrayFormat(message, objects).getMessage();
        LOGGER.warn(message, objects);
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