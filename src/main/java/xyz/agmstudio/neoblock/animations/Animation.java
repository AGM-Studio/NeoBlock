package xyz.agmstudio.neoblock.animations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.idle.IdleAnimation;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class Animation implements ConfigUtil.CategorizedConfig {
    private static boolean registeringNewAnimations = true;
    private static final List<Animation> animations = new ArrayList<>();

    public static void addAnimation(Animation animation) {
        animations.add(animation);
    }
    public static void clearAnimations() {
        animations.clear();
    }
    public static void disableRegisteringNewAnimations() {
        registeringNewAnimations = false;
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canRegisterNewAnimations() {
        return registeringNewAnimations;
    }

    public static void tickAll(ServerLevel level, LevelAccessor access) {
        animations.forEach(animation -> animation.tick(level, access));
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

    public String getPath() {
        return path;
    }

    @ConfigUtil.ConfigField
    protected boolean enabled;
    private final String path;

    public Animation(String path) {
        if (!path.isEmpty() && !path.endsWith(".")) path += ".";
        this.path = StringUtil.convertToSnakeCase(path);
    }

    public Animation(String category, String name) {
        this(createPath(category, name));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public final boolean register() {
        this.reload();
        if (!enabled) return false;
        Animation.addAnimation(this);
        onRegister();
        return true;
    }

    public final void reload() {
        ConfigUtil.loadValues(NeoBlockMod.getConfig(), this, true);
        processConfig();
    }

    protected abstract void onRegister();
    protected void processConfig() {}

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
