package xyz.agmstudio.neoblock.platform;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;

public interface IRegistryHelper {
    NeoMobSpec.TradeTicket getMobTicket();
    BlockItem getNeoBlockItem();
    Block getNeoBlock();
}
