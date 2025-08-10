package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.commands.*;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.compatibility.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@EventBusSubscriber(modid = NeoBlockMod.MOD_ID)
public final class NeoListener {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static <T> void execute(Callable<T> callable) {
        executor.submit(callable);
    }

    private static final HashSet<Consumer<ServerLevel>> tickers = new HashSet<>();

    public static void registerTicker(Consumer<ServerLevel> ticker) {
        tickers.add(ticker);
    }

    private static @Nullable ServerLevel getServerConditioned(LevelAccessor level, boolean isOverWorld, boolean isNotDisabled) {
        if (!(level instanceof ServerLevel server)) return null;
        if (isOverWorld && server.dimension() != Level.OVERWORLD) return null;
        if (isNotDisabled && WorldData.getWorldStatus().isDisabled()) return null;

        return server;
    }


    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.@NotNull Load event) {
        ServerLevel level = getServerConditioned(event.getLevel(), true, false);
        if (level != null) WorldData.setup(level);
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.@NotNull Post event) {
        ServerLevel level = getServerConditioned(event.getLevel(), true, true);
        if (level == null) return;

        tickers.forEach(ticker -> ticker.accept(level));

        final BlockState block = level.getBlockState(BlockManager.getBlockPos());
        if (WorldData.getWorldStatus().isUpdated() || TierManager.hasResearch()) {
            if (block.getBlock() != Blocks.BEDROCK) BlockManager.BEDROCK_SPEC.placeAt(level, BlockManager.getBlockPos());
        } else if (block.isAir() || block.canBeReplaced())          // NeoBlock has been broken logic
            BlockManager.updateBlock(level, true);
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        ServerLevel level = getServerConditioned(event.getLevel(), false, false);
        if (level == null) return;

        if (event.getEntity() instanceof WanderingTrader trader) NeoMerchant.handleTrader(trader);
        if (event.getEntity() instanceof ServerPlayer player) {
            if (TierManager.hasResearch()) TierManager.addPlayer(player);
            MessengerAPI.onPlayerJoin(level, player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new MainCommand();

        new DisableTierCommand();
        new EnableTierCommand();
        new CommandTierCommand();

        new ForceStopCommand();
        new ForceBlockCommand();
        new ForceResetTiersCommand();
        new ForceTraderSpawnCommand();

        new SchematicSaveCommand();
        new SchematicLoadCommand();

        NeoCommand.registerAll(event.getDispatcher());
    }
}
