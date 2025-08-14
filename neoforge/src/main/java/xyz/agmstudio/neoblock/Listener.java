package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
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
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        ServerLevel level = getServerConditioned(event.getLevel(), false, false);
        if (level != null) NeoBlock.onEntitySpawn(level, event.getEntity());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NeoBlock.registerCommands();
        NeoCommand.registerAll(event.getDispatcher());
    }
}
