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
import xyz.agmstudio.neoblock.neo.world.WorldData;
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

    public void setup(FMLCommonSetupEvent event) {
        NeoListener.registerTicker(Animation::tickAll);
        NeoListener.registerTicker(WorldData::tick);
    }
}