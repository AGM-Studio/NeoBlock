package xyz.agmstudio.neoblock.animations;

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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
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
        CommentedFileConfig config = NeoBlockMod.getConfig();
        this.enabled = config.getOrElse(getPath("enabled"), false);
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
                    } catch (IllegalAccessException e) {
                        NeoBlockMod.LOGGER.error("Failed to load animation config value for: {}", field.getName(), e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }

        processConfig();
    }

    protected abstract void onRegister();
    protected abstract void processConfig();

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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface AnimationConfig {
        String value() default "";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Register {}
}
