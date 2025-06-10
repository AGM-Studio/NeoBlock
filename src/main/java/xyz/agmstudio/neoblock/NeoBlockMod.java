package xyz.agmstudio.neoblock;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
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
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldUpgrade;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.function.Supplier;

@Mod(NeoBlockMod.MOD_ID)
public final class NeoBlockMod {
    public static final String MOD_ID = "neoblock";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> register = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return register;
    }

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
        this.install(bus, container);
    }

    private void install(IEventBus bus, ModContainer container) {
        NeoBlockMod.container = container;
        NeoBlockMod.instance = this;
        NeoBlockMod.config = ResourceUtil.getConfig(folder, "config.toml");

        bus.addListener(this::setup);

        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static void reload() {
        TierData.reload();

        NeoMerchant.loadConfig();
        NeoTrade.reloadTrades();

        Animation.clearAnimations();

        WorldUpgrade.reloadProgressbarAnimations();
        WorldUpgrade.clearPhaseAnimations();
        UpgradePhaseAnimation.getAnimations().forEach(Animation::register);
        WorldUpgrade.clearProgressAnimations();
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