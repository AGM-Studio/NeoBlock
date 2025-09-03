package xyz.agmstudio.neoblock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;


public abstract class NeoBlock {
    public static final String MOD_ID = "neoblock";
    public static final String MOD_NAME = "NeoBlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final Path CONFIG_FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID);

    public static final IRegistryHelper REGISTRY = Services.load(IRegistryHelper.class);

    private static NeoBlock instance;
    private static IConfig config;

    public static NeoBlock getInstance() {
        return instance;
    }
    public static IConfig getConfig() {
        return config;
    }

    public static void reloadConfig() {
        NeoBlock.config = ConfigUtil.getConfig(CONFIG_FOLDER, "config.toml");
    }

    protected NeoBlock(String name) {
        assert MOD_NAME.equals(name);

        NeoBlock.instance = this;
        NeoBlock.config = ConfigUtil.getConfig(CONFIG_FOLDER, "config.toml");

        // To make sure files & folders are created.
        WorldData.reloadConfig();
        ResourceUtil.loadAllTierConfigs();
        // Schematic class will take care of it.
        boolean ignored = Schematic.folder.toFile().exists();

        if (NeoBlock.isDevelopmentEnvironment()) {
            Configurator.setRootLevel(Level.ERROR);
            Configurator.setLevel(LOGGER.getName(), Level.DEBUG);
            LOGGER.debug("Enabling debug mode for neoblock (development environment)");
        }

        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(BlockManager::tick);
        NeoListener.registerTicker(TierManager::tick);

        NeoMobSpec.load();
    }

    protected abstract String getPlatformNameImpl();
    protected abstract boolean isModLoadedImpl(String modId);
    protected abstract boolean isDevelopmentEnvironmentImpl();
    protected abstract Path getConfigFolderImpl();
    protected abstract <T extends SavedData> T captureSavedDataImpl(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator);
    protected abstract WorldData instanceWorldDataImpl(ServerLevel level);

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
    public static WorldData instanceWorldData(ServerLevel level) {
	    return instance.instanceWorldDataImpl(level);
    }
}