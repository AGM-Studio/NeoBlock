package xyz.agmstudio.neoblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.platform.NeoForgeMC;
import xyz.agmstudio.neoblock.platform.NeoForgeRegistry;
import xyz.agmstudio.neocore.NeoMod;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(NeoBlockMod.MOD_ID)
public final class ImplMod extends NeoBlockMod {
    static { // Should be in NeoCore in future
        NeoMod.MC = new NeoForgeMC();
    }

    public ImplMod(IEventBus bus, ModContainer container) {
        super(new NeoForgeRegistry());

        NeoForgeRegistry.BLOCKS.register(bus);
        NeoForgeRegistry.ITEMS.register(bus);

        NeoForge.EVENT_BUS.register(ImplMod.class);
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.@NotNull Load event) {
        NeoListener.onWorldLoad(event.getLevel());
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.@NotNull Post event) {
        NeoListener.onWorldTick(event.getLevel());
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
        NeoListener.onRegisterCommands(event.getBuildContext(), event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        NeoListener.LivingDamageResult result = NeoListener.onLivingDamage(event.getEntity(), event.getSource(), event.getNewDamage());
        if (result == null) return;
        if (result.isCanceled()) event.setNewDamage(0.0F);
        else event.setNewDamage(result.getResult());
    }

    @Override public <T extends SavedData> T captureSavedDataImpl(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator) {
        BiFunction<CompoundTag, HolderLookup.Provider, T> neoLoader = ((tag, provider) -> loader.apply(tag));
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(creator, neoLoader), name);
    }
    @Override
    public WorldManager instanceWorldDataImpl(ServerLevel level) {
        return new WorldManager(level) {
            @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
                return saveDataOnTag(tag);
            }
        };
    }
}