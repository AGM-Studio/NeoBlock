package xyz.agmstudio.neoblock.platform;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;

public class NeoForgeRegistry implements IRegistryHelper {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeoBlock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeoBlock.MOD_ID);

    public static final DeferredItem<NeoMobSpec.TradeTicket> TICKET =
            ITEMS.register("mob_ticket", () -> new NeoMobSpec.TradeTicket(new Item.Properties().stacksTo(16)));

    @Override public NeoMobSpec.TradeTicket getMobTicket() {
        return TICKET.get();
    }
}
