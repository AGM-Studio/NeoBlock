package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.platform.ForgeRegistry;

@Mod(NeoBlock.MOD_ID)
public final class ImplMod extends NeoBlock {
    public ImplMod() {
        super(NeoBlock.MOD_NAME);

        IEventBus bus = MinecraftForge.EVENT_BUS;

        ForgeRegistry.BLOCKS.register(bus);
        ForgeRegistry.ITEMS.register(bus);

        MinecraftForge.EVENT_BUS.register(ImplMod.class);
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.@NotNull Load event) {
        NeoListener.onWorldLoad(event.getLevel());
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        NeoListener.onWorldTick(event.level);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player)
            NeoListener.onBlockBroken(event.getLevel(), player, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        NeoListener.onEntitySpawn(event.getLevel(), event.getEntity());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NeoListener.onRegisterCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        NeoListener.LivingDamageResult result = NeoListener.onLivingDamage(event.getEntity(), event.getSource(), event.getAmount());
        if (result == null) return;
        if (result.isCanceled()) event.setCanceled(true);
        else event.setAmount(result.getResult());
    }
}