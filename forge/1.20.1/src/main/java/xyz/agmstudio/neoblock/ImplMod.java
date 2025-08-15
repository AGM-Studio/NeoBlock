package xyz.agmstudio.neoblock;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import xyz.agmstudio.neoblock.platform.ForgeRegistry;

@Mod(NeoBlock.MOD_ID)
public final class ImplMod extends NeoBlock {
    public ImplMod() {
        super(NeoBlock.MOD_NAME);

        IEventBus bus = MinecraftForge.EVENT_BUS;

        ForgeRegistry.BLOCKS.register(bus);
        ForgeRegistry.ITEMS.register(bus);
    }
}