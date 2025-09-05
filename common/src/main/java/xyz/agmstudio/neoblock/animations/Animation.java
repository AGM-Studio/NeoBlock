package xyz.agmstudio.neoblock.animations;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.animations.idle.IdleAnimation;
import xyz.agmstudio.neoblock.animations.idle.NeoFlowAnimation;
import xyz.agmstudio.neoblock.animations.idle.PulseAnimation;
import xyz.agmstudio.neoblock.animations.phase.ExplosionAnimation;
import xyz.agmstudio.neoblock.animations.phase.FuseAnimation;
import xyz.agmstudio.neoblock.animations.progress.BreakingAnimation;
import xyz.agmstudio.neoblock.animations.progress.SparkleAnimation;
import xyz.agmstudio.neoblock.animations.progress.SpiralAnimation;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class Animation implements IConfig.Configured {
    private static final Path FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID, "animations");
    private static final List<Animation> animations = new ArrayList<>();

    public static void reloadAnimations() {
        animations.clear();

        TierManager.clearPhaseAnimations();
        TierManager.clearProgressAnimations();
        TierManager.reloadProgressbarAnimations();

        new ExplosionAnimation().register();
        new FuseAnimation().register();

        new BreakingAnimation().register();
        new SparkleAnimation().register();
        new SpiralAnimation().register();

        new NeoFlowAnimation().register();
        new PulseAnimation().register();
    }

    public static void tickAll(ServerLevel level) {
        animations.forEach(animation -> animation.tick(level));
    }
    public static void resetIdleTick() {
        for (Animation animation : animations)
            if (animation instanceof IdleAnimation idle)
                idle.resetTick();
    }

    private static String createPath(String category, String name) {
        if (category.contains(" ")) category = "\"%s\"".formatted(category);
        if (name.contains(" ")) name = "\"%s\"".formatted(name);
        return "animations." + category + "." + name + ".";
    }

    @Override public IConfig getConfig() {
        return this.config;
    }

    @ConfigField
    protected boolean enabled;
    private final IConfig config;

    public Animation(@NotNull IConfig config) {
        this.config = config;
    }
    public Animation(String name) {
        this.config = IConfig.getConfig(FOLDER, name);
    }
    public Animation(String category, String name) {
        final Path subfolder = FOLDER.resolve(category);
        this.config = IConfig.getConfig(subfolder, name);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public final boolean register() {
        this.reload();
        if (!enabled) return false;
        animations.add(this);
        onRegister();
        return true;
    }

    public final void reload() {
        loadValues(true);
        processConfig();
    }

    protected abstract void onRegister();
    protected void processConfig() {}

    /**
     * Will always tick...
     *
     * @param level the level to play animation
     */
    public void tick(ServerLevel level) {}

    /**
     * Should animate the animation!
     *
     * @param level the level to play animation
     */
    public void animate(ServerLevel level) {}

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        Class<?> clazz = this.getClass();

        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if ((field.getModifiers() & 0x00000008) != 0 || field.getName().equals("path")) continue;
                try {
                    sb.append(field.getName()).append(": ").append(field.get(this)).append(", ");
                } catch (IllegalAccessException e) {
                    sb.append(field.getName()).append(field.getName()).append(": NO_ACCESS, ");
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return getClass().getSimpleName() + "{" + sb + "}";
    }
}
