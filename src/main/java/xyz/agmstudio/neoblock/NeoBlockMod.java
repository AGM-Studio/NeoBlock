package xyz.agmstudio.neoblock;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.UpgradeManager;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.animations.Animation;
import xyz.agmstudio.neoblock.tiers.animations.phase.ExplosionAnimation;
import xyz.agmstudio.neoblock.tiers.animations.phase.FuseAnimation;
import xyz.agmstudio.neoblock.tiers.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.BreakingAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.SparkleAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.SpiralAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;

@Mod(NeoBlockMod.MOD_ID)
public final class NeoBlockMod {
    public static final String MOD_ID = "neoblock";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final Path folder = ResourceUtil.getConfigFolder(NeoBlockMod.MOD_ID);
    public static Path getFolder() {
        return folder;
    }

    private static ModContainer container;
    private static NeoBlockMod instance;
    private static CommentedFileConfig config;

    public static ModContainer getContainer() {
        return container;
    }
    public static NeoBlockMod getInstance() {
        return instance;
    }
    public static CommentedFileConfig getConfig() {
        return config;
    }

    public NeoBlockMod(IEventBus bus, ModContainer container) {
        NeoBlockMod.container = container;
        NeoBlockMod.instance = this;
        NeoBlockMod.config = ResourceUtil.getConfig(folder, "config.toml");

        bus.addListener(this::setup);
        NeoForge.EVENT_BUS.addListener(MessagingUtil::onPlayerJoin);
    }

    public static void reload() {
        NeoBlock.reload();

        NeoMerchant.loadConfig();

        UpgradeManager.reloadProgressbarAnimations();
        UpgradeManager.clearPhaseAnimations();
        UpgradePhaseAnimation.getAnimations().forEach(Animation::register);
        UpgradeManager.clearProgressAnimations();
        UpgradeProgressAnimation.getAnimations().forEach(Animation::register);
    }

    public void setup(FMLCommonSetupEvent event) {
        UpgradePhaseAnimation.addAnimation(ExplosionAnimation.class);
        UpgradePhaseAnimation.addAnimation(FuseAnimation.class);

        UpgradeProgressAnimation.addAnimation(BreakingAnimation.class);
        UpgradeProgressAnimation.addAnimation(SparkleAnimation.class);
        UpgradeProgressAnimation.addAnimation(SpiralAnimation.class);

        Animation.disableRegisteringNewAnimations();

        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(WorldData::tick);

        NeoBlockMod.reload();
    }
}