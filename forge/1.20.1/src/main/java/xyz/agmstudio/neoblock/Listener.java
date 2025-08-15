package xyz.agmstudio.neoblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.WorldData;

@Mod.EventBusSubscriber(modid = NeoBlock.MOD_ID)
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
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        ServerLevel level = getServerConditioned(event.level, true, true);
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
