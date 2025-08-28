package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;

@EventBusSubscriber(modid = NeoBlock.MOD_ID)
public final class Listener {
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
        if (level != null) NeoBlock.onTick(level);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer().isCreative()) return;
        if (event.getPlayer() instanceof ServerPlayer player && BlockManager.isNeoBlock(event.getPos()))
            WorldData.addBlocksBroken(player, 1);
        if (event.getState().getBlock() == Blocks.END_PORTAL_FRAME)
            BlockManager.handleEndPortalFrameBreak(level, event.getState(), event.getPos(), event.getPlayer());
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        ServerLevel level = getServerConditioned(event.getLevel(), false, false);
        if (level != null) NeoBlock.onEntitySpawn(level, event.getEntity());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NeoBlock.registerCommands();
        NeoCommand.registerAll(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        ServerLevel level = getServerConditioned(event.getEntity().level(), false, false);
        if (level == null) return;

        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD))
            if (ForgivingVoid.handleVoid(level, event.getEntity())) event.setNewDamage(0);
    }
}
