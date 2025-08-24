package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.*;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.MessengerUtil;
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

        NeoBlock.LOGGER.warn("Enabling the neoblock...");
        if (Services.PLATFORM.isDevelopmentEnvironment()) {
            Configurator.setRootLevel(Level.ERROR);
            Configurator.setLevel(LOGGER.getName(), Level.DEBUG);
            LOGGER.debug("Disabling other mods logging and enabling debug mode for neoblock (development environment)");
        }

        NeoBlock.registerTicker(Animation::tickAll);
        NeoBlock.registerTicker(BlockManager::tick);
        NeoBlock.registerTicker(TierManager::tick);

        NeoMobSpec.load();
    }

    public static void registerCommands() {
        new MainCommand();

        new DisableTierCommand();
        new EnableTierCommand();
        new CommandTierCommand();

        new ForceStopCommand();
        new ForceBlockCommand();
        new ForceResetTiersCommand();
        new ForceTraderSpawnCommand();

        new SchematicSaveCommand();
        new SchematicLoadCommand();
    }

    public static void onEntitySpawn(ServerLevel level, Entity entity) {
        if (entity instanceof WanderingTrader trader) NeoMerchant.handleTrader(trader);
        if (entity instanceof ServerPlayer player) {
            if (TierManager.hasResearch()) TierManager.addPlayer(player);
            MessengerUtil.onPlayerJoin(level, player);
        }
    }
    public static void onTick(ServerLevel level) {
        tickers.forEach(ticker -> ticker.accept(level));
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static <T> void execute(Callable<T> callable) {
        executor.submit(callable);
    }

    private static final HashSet<Consumer<ServerLevel>> tickers = new HashSet<>();
    public static void registerTicker(Consumer<ServerLevel> ticker) {
        tickers.add(ticker);
    }
}