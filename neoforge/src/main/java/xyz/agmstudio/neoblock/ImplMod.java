package xyz.agmstudio.neoblock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import xyz.agmstudio.neoblock.platform.NeoForgeRegistry;

import java.util.function.Supplier;

@Mod(NeoBlock.MOD_ID)
public final class ImplMod extends NeoBlock {
    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> register = NeoForgeRegistry.BLOCKS.register(name, block);
        NeoForgeRegistry.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return register;
    }

    public ImplMod(IEventBus bus, ModContainer container) {
        super(NeoBlock.MOD_NAME);

        NeoForgeRegistry.BLOCKS.register(bus);
        NeoForgeRegistry.ITEMS.register(bus);
    }
}