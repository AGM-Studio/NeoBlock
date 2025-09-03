package xyz.agmstudio.neoblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.ForgeRegistry;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @Override public String getPlatformNameImpl() {
        return "Forge";
    }
    @Override public boolean isModLoadedImpl(String modId) {
        return ModList.get().isLoaded(modId);
    }
    @Override public boolean isDevelopmentEnvironmentImpl() {
        return !FMLLoader.isProduction();
    }
    @Override public Path getConfigFolderImpl() {
        return FMLPaths.CONFIGDIR.get();
    }
    @Override public <T extends SavedData> T captureSavedDataImpl(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator) {
        BiFunction<CompoundTag, HolderLookup.Provider, T> neoLoader = ((tag, provider) -> loader.apply(tag));
        //noinspection DataFlowIssue
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(creator, neoLoader, null), name);    }
    @Override
    public WorldData instanceWorldDataImpl(ServerLevel level) {
        return new WorldData(level) {
            @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
                return saveDataOnTag(tag);
            }
        };
    }
}