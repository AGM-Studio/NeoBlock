package xyz.agmstudio.neoblock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.function.Supplier;

@Mod(NeoBlock.MOD_ID)
public final class ImplMod extends NeoBlock {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> register = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return register;
    }

    private static final Path folder = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID);

    public ImplMod(IEventBus bus, ModContainer container) {
        super(NeoBlock.MOD_NAME);

        NeoBlock.registerTicker(Animation::tickAll);
        NeoBlock.registerTicker(WorldData::tick);

        NeoMobSpec.load();

        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}