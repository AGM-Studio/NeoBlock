package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public abstract class Animation {
    private static final List<Animation> animations = new ArrayList<>();

    public Animation() {
        animations.add(this);
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.@NotNull Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        animations.forEach(animation -> animation.tick(level, event.getLevel()));
    }

    /**
     * Will always tick...
     *
     * @param level the level to play animation
     * @param access the world access if needed
     */
    public void tick(ServerLevel level, LevelAccessor access) {}

    /**
     * Should animate the animation!
     *
     * @param level the level to play animation
     * @param access the world access if needed
     */
    public void animate(ServerLevel level, LevelAccessor access) {}
}
