package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;

import java.util.List;

public class NeoForgeRegistry implements IRegistryHelper {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeoBlock.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeoBlock.MOD_ID);

    public static ResourceKey<Item> createItemKey(String name) {
        return ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NeoBlock.MOD_ID, name));
    }

    public static final DeferredItem<NeoMobSpec.TradeTicket> TICKET =
            ITEMS.register("mob_ticket", () -> new NeoMobSpec.TradeTicket(new Item.Properties().stacksTo(16).setId(createItemKey("mob_ticket"))) {
                @Override public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
                    components.addAll(getLore(stack));
                }
            });
    public static final DeferredBlock<Block> NEOBLOCK_BLOCK =
            BLOCKS.registerSimpleBlock("neoblock");
    public static final DeferredItem<BlockItem> NEOBLOCK_BLOCK_ITEM =
            ITEMS.register("neoblock", () -> new BlockItem(NEOBLOCK_BLOCK.get(), new Item.Properties().setId(createItemKey("neoblock")))
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
