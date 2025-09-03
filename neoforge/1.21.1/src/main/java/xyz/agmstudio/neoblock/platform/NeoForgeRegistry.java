package xyz.agmstudio.neoblock.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;

import java.util.List;

public class NeoForgeRegistry implements IRegistryHelper {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeoBlock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeoBlock.MOD_ID);

    public static final DeferredItem<NeoMobSpec.TradeTicket> TICKET =
            ITEMS.register("mob_ticket", () -> new NeoMobSpec.TradeTicket(new Item.Properties().stacksTo(16)) {
                @Override public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
                    components.addAll(getLore(stack));
                }
            });
    public static final DeferredBlock<Block> NEOBLOCK_BLOCK =
            BLOCKS.registerSimpleBlock("neoblock");
    public static final DeferredItem<BlockItem> NEOBLOCK_BLOCK_ITEM =
            ITEMS.register("neoblock", () -> new BlockItem(NEOBLOCK_BLOCK.get(), new Item.Properties())
    );

    @Override public NeoMobSpec.TradeTicket getMobTicket() {
        return TICKET.get();
    }
    @Override public Block getNeoBlock() {
        return NEOBLOCK_BLOCK.get();
    }
    @Override public BlockItem getNeoBlockItem() {
        return NEOBLOCK_BLOCK_ITEM.get();
    }
}
