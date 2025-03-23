package xyz.agmstudio.neoblock.tiers.animations;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class Animation {
    private static boolean registeringNewAnimations = true;
    private static final List<Animation> animations = new ArrayList<>();

    public static void addAnimation(Animation animation) {
        animations.add(animation);
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

    private static String createPath(String category, String name) {
        if (category.contains(" ")) category = "\"%s\"".formatted(category);
        if (name.contains(" ")) name = "\"%s\"".formatted(name);
        return "animations." + category + "." + name + ".";
    }
    protected String getPath(String label) {
        return path + StringUtil.convertToSnakeCase(label);
    }

    private final String path;
    protected boolean enabled;

    public Animation(String path) {
        if (!path.isEmpty() && !path.endsWith(".")) path += ".";
        this.path = StringUtil.convertToSnakeCase(path);

        CommentedFileConfig config = NeoBlockMod.getConfig();
        this.enabled = config.getOrElse(getPath("enabled"), false);
        StringBuilder debug = new StringBuilder("Loaded animation: " + path + "\n\tenabled: " + enabled);
        Class<?> clazz = this.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(AnimationConfig.class)) {
                    AnimationConfig annotation = field.getAnnotation(AnimationConfig.class);
                    String label = annotation.value().isEmpty() ? field.getName() : annotation.value();
                    String fullPath = getPath(label);

                    try {
                        field.setAccessible(true);
                        Object def = field.get(this);
                        Object value = config.getOrElse(fullPath, def);
                        switch (value) {
                            case Double v when field.getType() == float.class -> field.set(this, v.floatValue());
                            case Number number when field.getType() == int.class -> field.set(this, number.intValue());
                            case null, default -> field.set(this, value);
                        }

                        debug.append("\n\t").append(field.getName()).append(": ").append(value);
                    } catch (IllegalAccessException e) {
                        NeoBlockMod.LOGGER.error("Failed to load animation config value for: {}", field.getName(), e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }

        processConfig();
        NeoBlockMod.LOGGER.debug(debug.toString());
    }

    public Animation(String category, String name) {
        this(createPath(category, name));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean register() {
        if (!enabled) return false;
        Animation.addAnimation(this);
        return true;
    }
    public void processConfig() {}

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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface AnimationConfig {
        String value() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Register {}
}
