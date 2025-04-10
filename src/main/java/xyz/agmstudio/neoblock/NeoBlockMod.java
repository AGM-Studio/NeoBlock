package xyz.agmstudio.neoblock;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.animations.idle.IdleAnimation;
import xyz.agmstudio.neoblock.animations.idle.NeoFlowAnimation;
import xyz.agmstudio.neoblock.animations.idle.PulseAnimation;
import xyz.agmstudio.neoblock.animations.phase.ExplosionAnimation;
import xyz.agmstudio.neoblock.animations.phase.FuseAnimation;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.BreakingAnimation;
import xyz.agmstudio.neoblock.animations.progress.SparkleAnimation;
import xyz.agmstudio.neoblock.animations.progress.SpiralAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.tiers.TierManager;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
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

    public NeoBlockMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContainer container = ModList.get().getModContainerById(MOD_ID).get();
        this.install(bus, container);
    }

    public NeoBlockMod(IEventBus bus, ModContainer container) {
        this.install(bus, container);
    }

    private void install(IEventBus bus, ModContainer container) {
        NeoBlockMod.container = container;
        NeoBlockMod.instance = this;
        NeoBlockMod.config = ResourceUtil.getConfig(folder, "config.toml");

        bus.addListener(this::setup);
    }

    public static void reload() {
        TierManager.reload();

        NeoMerchant.loadConfig();

        Animation.clearAnimations();

        TierManager.reloadProgressbarAnimations();
        TierManager.clearPhaseAnimations();
        UpgradePhaseAnimation.getAnimations().forEach(Animation::register);
        TierManager.clearProgressAnimations();
        UpgradeProgressAnimation.getAnimations().forEach(Animation::register);

        IdleAnimation.getAnimations().forEach(Animation::register);
    }

    public void setup(FMLCommonSetupEvent event) {
        UpgradePhaseAnimation.addAnimation(ExplosionAnimation.class);
        UpgradePhaseAnimation.addAnimation(FuseAnimation.class);

        UpgradeProgressAnimation.addAnimation(BreakingAnimation.class);
        UpgradeProgressAnimation.addAnimation(SparkleAnimation.class);
        UpgradeProgressAnimation.addAnimation(SpiralAnimation.class);

        IdleAnimation.addAnimation(NeoFlowAnimation.class);
        IdleAnimation.addAnimation(PulseAnimation.class);

        Animation.disableRegisteringNewAnimations();

        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(WorldData::tick);

        NeoBlockMod.reload();
    }
}