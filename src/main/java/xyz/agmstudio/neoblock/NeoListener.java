package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber(modid = NeoBlockMod.MOD_ID)
public final class NeoListener {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.@NotNull Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        NeoBlock.setupWorldData(level);
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.@NotNull Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        NeoBlock.setupWorldData(null);
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.@NotNull Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        final LevelAccessor access = event.getLevel();

        // Upgrading the neoblock... Nothing else should happen meanwhile
        if (NeoBlock.UPGRADE == null) NeoBlock.UPGRADE = NeoBlock.DATA.fetchUpgrade();
        if (NeoBlock.UPGRADE == null) NeoBlockMod.LOGGER.info("NeoBlock upgrade not available!");
        else if (NeoBlock.UPGRADE.isOnUpgrade()) {
            NeoBlock.UPGRADE.tick(level, access);
            return;
        }

        // Merchant tick
        if (tickCounter++ % 20 == 0) executor.submit(() -> NeoMerchant.tick(level));
        if (tickCounter > 1000) tickCounter = 0;

        // NeoBlock has been broken logic
        BlockState block = access.getBlockState(NeoBlock.POS);
        if (!block.isAir() && !block.canBeReplaced()) return;
        NeoBlock.onBlockBroken(level, access, true);

        executor.submit(() -> NeoMerchant.attemptSpawnTrader(level));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.@NotNull BreakEvent event) {
        if (!event.getPos().equals(NeoBlock.POS) || event.getLevel().isClientSide()) return;
        NeoBlock.DATA.addPlayerBlockCount(event.getPlayer(), 1);
    }

    @SubscribeEvent
    public static void onWanderingSpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof WanderingTrader trader) NeoMerchant.handleTrader(trader);
        if (event.getEntity() instanceof ServerPlayer player && NeoBlock.UPGRADE.isOnUpgrade()) NeoBlock.UPGRADE.showTo(player);
    }
}
