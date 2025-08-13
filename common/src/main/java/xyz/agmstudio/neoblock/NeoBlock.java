package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


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
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static <T> void execute(Callable<T> callable) {
        executor.submit(callable);
    }

    private static final HashSet<Consumer<ServerLevel>> tickers = new HashSet<>();
    public static void registerTicker(Consumer<ServerLevel> ticker) {
        tickers.add(ticker);
    }

    public static void tick(ServerLevel level) {
        tickers.forEach(ticker -> ticker.accept(level));
    }

    public static @Nullable ServerLevel getServerConditioned(LevelAccessor level, boolean isOverWorld, boolean isNotDisabled) {
        if (!(level instanceof ServerLevel server)) return null;
        if (isOverWorld && server.dimension() != Level.OVERWORLD) return null;
        if (isNotDisabled && WorldData.getWorldStatus().isDisabled()) return null;

        return server;
    }
}