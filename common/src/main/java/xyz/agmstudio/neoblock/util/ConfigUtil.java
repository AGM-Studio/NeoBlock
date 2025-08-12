package xyz.agmstudio.neoblock.util;

import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.platform.helpers.IConfigHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class ConfigUtil {
    /**
     * Returns the {@link IConfig} corresponding to name in folder.
     * If missing will try to load from resources.
     *
     * @param folder the folder to look up
     * @param name the config name
     * @return the config (see {@link IConfig}
     */
    public static IConfig getConfig(Path folder, String name) {
        if (!folder.toFile().exists()) try {
            Files.createDirectories(folder);
        } catch (IOException ignored) {}
        Path configPath = folder.resolve(name.endsWith(".toml") ? name : name + ".toml");
        if (!Files.exists(configPath)) try {
            Path path = ResourceUtil.pathOf(NeoBlock.MOD_ID);
            String resource = configPath.toAbsolutePath().toString().replace(path.toAbsolutePath().toString(), "\\configs");
            NeoBlock.LOGGER.debug("Loading resource {} for {}", resource, configPath);
            ResourceUtil.processResourceFile(resource, configPath, new HashMap<>());
        } catch (Exception ignored) {}
        if (!Files.exists(configPath)) return null;

        IConfig config = IConfigHelper.getIConfig(configPath);

        config.load();
        return config;
    }

    public interface CategorizedConfig {
        String getPath();
    }

    private static String getPath(Object instance, String label) {
        String path = (instance instanceof CategorizedConfig categorized) ? categorized.getPath() : "";
        if (!path.isEmpty() && !path.endsWith(".")) path += ".";
        return path + label;
    }

    public static void loadValues(@NotNull IConfig config, @NotNull Object instance) {
        loadValues(config, instance, false);
    }
    public static void loadValues(@NotNull IConfig config, @NotNull Object instance, boolean deep) {
        Class<?> clazz = instance.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ConfigField.class)) {
                    ConfigField annotation = field.getAnnotation(ConfigField.class);
                    String label = annotation.value().isEmpty() ? field.getName() : annotation.value();
                    Object value = null;
                    for (String name: label.split("\\|")) {
                        String path = getPath(instance, name);
                        value = config.get(path);
                        if (IConfigHelper.isINull(value)) break;
                    }

                    try {
                        field.setAccessible(true);
                        Object def = field.get(instance);
                        Object result = IConfigHelper.isINull(value) ? value : def;
                        if (result instanceof Number num) {
                            double min = annotation.min();
                            double max = annotation.max();
                            if (!Double.isNaN(min) && num.doubleValue() < min) num = min;
                            if (!Double.isNaN(max) && num.doubleValue() > max) num = max;
                            if (field.getType() == byte.class) field.set(instance, num.byteValue());
                            else if (field.getType() == short.class) field.set(instance, num.shortValue());
                            else if (field.getType() == int.class) field.set(instance, num.intValue());
                            else if (field.getType() == long.class) field.set(instance, num.longValue());
                            else if (field.getType() == float.class) field.set(instance, num.floatValue());
                            else if (field.getType() == double.class) field.set(instance, num.doubleValue());
                            else field.set(instance, def);
                        } else try {
                            field.set(instance, cast(field.getType(), value, def));
                        } catch (Exception ignored) {
                            field.set(instance, value != null ? value : def);
                        }
                    } catch (Exception e) {
                        NeoBlock.LOGGER.error("Failed to load config value for \"{}\" from \"{}\"", field.getName(), label, e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
        } while (deep && clazz != null);
    }

    @SuppressWarnings("unchecked")
    private static <R> R cast(Class<? extends R> type, Object value, R def) {
        try {
            Method factoryMethod = type.getDeclaredMethod("fromConfig", value.getClass());
            return (R) factoryMethod.invoke(null, value);
        } catch (Exception ignored) {}
        try {
            return (R) value;
        } catch (Exception ignored) {
            return def;
        }
    }
    private static <R> R cast(Class<? extends R> type, Object value) {
        return cast(type, value, null);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigField {
        String value() default "";          // The path to the value
        double min() default Double.NaN;    // For the numbers!
        double max() default Double.NaN;
    }
}
