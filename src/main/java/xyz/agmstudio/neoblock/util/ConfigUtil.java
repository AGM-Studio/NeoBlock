package xyz.agmstudio.neoblock.util;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class ConfigUtil {
    public interface CategorizedConfig {
        String getPath();
    }

    private static String getPath(Object instance, String label) {
        String path = (instance instanceof CategorizedConfig categorized) ? categorized.getPath() : "";
        if (!path.isEmpty() && !path.endsWith(".")) path += ".";
        return path + label;
    }

    public static void loadValues(@NotNull UnmodifiableConfig config, @NotNull Object instance) {
        loadValues(config, instance, false);
    }
    public static void loadValues(@NotNull UnmodifiableConfig config, @NotNull Object instance, boolean deep) {
        Class<?> clazz = instance.getClass();
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ConfigField.class)) {
                    ConfigField annotation = field.getAnnotation(ConfigField.class);
                    String label = annotation.value().isEmpty() ? field.getName() : annotation.value();
                    String path = getPath(instance, label);

                    try {
                        field.setAccessible(true);
                        Object def = field.get(instance);
                        Object value = config.getOrElse(path, def);
                        if (value instanceof Number num) {
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
                        } else field.set(instance, value);
                    } catch (Exception e) {
                        NeoBlockMod.LOGGER.error("Failed to load config value for \"{}\" from \"{}\"", field.getName(), path, e);
                    }
                }
            }

            clazz = clazz.getSuperclass();
        } while (deep && clazz != null);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigField {
        String value() default "";          // The path to the value
        double min() default Double.NaN;    // For the numbers!
        double max() default Double.NaN;
    }
}
