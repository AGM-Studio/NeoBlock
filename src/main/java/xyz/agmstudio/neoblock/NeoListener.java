package xyz.agmstudio.neoblock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.commands.MainCommand;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.tiers.merchants.NeoOffer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber(modid = NeoBlockMod.MOD_ID)
public final class NeoListener {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.@NotNull Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        NeoBlock.setupWorldData(level);
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.@NotNull Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || NeoBlock.DATA.isDisabled()) return;
        final LevelAccessor access = event.getLevel();
        final BlockState block = access.getBlockState(NeoBlock.POS);

        // Upgrading the neoblock... Nothing else should happen meanwhile
        if (NeoBlock.DATA.isUpdated() || NeoBlock.isOnUpgrade()) {
            if (block.getBlock() != Blocks.BEDROCK) NeoBlock.setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());
            if (NeoBlock.isOnUpgrade()) NeoBlock.DATA.getUpgradeManager().tick(level, access);  // Todo better ticking
            return;
        }

        // Handle items in inventory
        for (Player player: level.players()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.isEmpty() && NeoOffer.handlePossibleMobTrade(stack)) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    player.getInventory().setChanged();
                }
            }
        }

        // NeoBlock has been broken logic
        if (!block.isAir() && !block.canBeReplaced()) return;
        NeoBlock.onBlockBroken(level, access, true);

        executor.submit(() -> NeoMerchant.attemptSpawnTrader(level));
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof WanderingTrader trader) NeoMerchant.handleTrader(trader);
        if (event.getEntity() instanceof ServerPlayer player && NeoBlock.isOnUpgrade()) NeoBlock.DATA.getUpgradeManager().addPlayer(player);
        if (event.getEntity() instanceof ItemEntity item && NeoOffer.handlePossibleMobTrade(item.getItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MainCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        EntityType<?> mob = NeoOffer.getMobTradeEntity(event.getItemStack());
        if (mob == null) return;
        event.getToolTip().add(
                Component.translatable("tooltip.neoblock.spawn_lore", mob.getDescription())
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
        );
    }
}
