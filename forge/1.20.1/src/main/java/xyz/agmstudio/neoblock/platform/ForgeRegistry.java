package xyz.agmstudio.neoblock.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.platform.helpers.IRegistryHelper;

import java.util.List;

public class ForgeRegistry implements IRegistryHelper {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NeoBlock.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NeoBlock.MOD_ID);

    public static final RegistryObject<NeoMobSpec.TradeTicket> TICKET =
            ITEMS.register("mob_ticket", () -> new NeoMobSpec.TradeTicket(new Item.Properties().stacksTo(16)) {
                @Override public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
                    components.addAll(getLore(stack));
                }
            });
    public static final RegistryObject<Block> NEOBLOCK_BLOCK =
            BLOCKS.register("neoblock", () -> new Block(BlockBehaviour.Properties.of()));
    public static final RegistryObject<BlockItem> NEOBLOCK_BLOCK_ITEM =
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
