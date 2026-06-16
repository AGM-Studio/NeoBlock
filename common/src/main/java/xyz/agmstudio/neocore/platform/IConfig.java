package xyz.agmstudio.neocore.platform;

import xyz.agmstudio.neocore.NeoMod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiConsumer;

public interface IConfig {
    class ConfigPath {
        private final String[] paths;
        private ConfigPath(String... paths) {
            this.paths = paths;
        }

        public String[] getPaths() {
            return paths;
        }

        public static ConfigPath of(String... paths) {
            return new ConfigPath(paths);
        }
    }

    default IConfig getSection(ConfigPath path) {
        for (String p: path.getPaths()) {
            IConfig result = getSection(p);
            if (NeoMod.isNull(result)) continue;
            return result;
        }

        return null;
    }
    IConfig getSection(String path);

    default boolean contains(ConfigPath path) {
        for (String p: path.getPaths()) if (contains(p)) return true;
        return false;
    }
    boolean contains(String path);

    default <T> T get(ConfigPath path) {
        return get(path, null);
    }
    default  <T> T get(ConfigPath path, T defaultValue) {
        for (String p: path.getPaths()) {
            T result = get(p);
            if (NeoMod.isNull(result)) continue;
            return result;
        }

        return defaultValue;
    }
    <T> T get(String path);
    <T> T get(String path, T defaultValue);

    default int getInt(ConfigPath path) {
        return this.<Number> get(path).intValue();
    }
    default int getInt(ConfigPath path, int defaultValue) {
        Number n = get(path);
        return n == null ? defaultValue : n.intValue();
    }
    default int getInt(String path) {
        return this.<Number> get(path).intValue();
    }
    default int getInt(String path, int defaultValue) {
        Number n = get(path);
        return n == null ? defaultValue : n.intValue();
    }

    void load();
    Set<String> keys();
    Set<String> sections();
    void forEach(BiConsumer<String, Object> action);

    interface Configured {
        @SuppressWarnings("unchecked")
        static <R> R cast(Class<? extends R> type, Object value, R def) {
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

        static <R> R cast(Class<? extends R> type, Object value) {
            return cast(type, value, null);
        }

        IConfig getConfig();

        default void loadValues() {
            loadValues(false);
        }
        default void loadValues(boolean deep) {
            Class<?> clazz = this.getClass();
            do {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Configured.ConfigField.class)) {
                        Configured.ConfigField annotation = field.getAnnotation(Configured.ConfigField.class);
                        String label = annotation.value().isEmpty() ? field.getName() : annotation.value();
                        Object value = getConfig().get(ConfigPath.of(label.split("\\|")));

                        try {
                            field.setAccessible(true);
                            Object def = field.get(this);
                            Object result = NeoMod.isNull(value) ? value : def;
                            if (result instanceof Number num) {
                                double min = annotation.min();
                                double max = annotation.max();
                                if (!Double.isNaN(min) && num.doubleValue() < min) num = min;
                                if (!Double.isNaN(max) && num.doubleValue() > max) num = max;
                                if (field.getType() == byte.class) field.set(this, num.byteValue());
                                else if (field.getType() == short.class) field.set(this, num.shortValue());
                                else if (field.getType() == int.class) field.set(this, num.intValue());
                                else if (field.getType() == long.class) field.set(this, num.longValue());
                                else if (field.getType() == float.class) field.set(this, num.floatValue());
                                else if (field.getType() == double.class) field.set(this, num.doubleValue());
                                else field.set(this, def);
                            } else try {
                                field.set(this, cast(field.getType(), value, def));
                            } catch (Exception ignored) {
                                //noinspection ConstantValue
                                field.set(this, value != null ? value : def);
                            }
                        } catch (Exception e) {
                            NeoMod.CORE_LOGGER.error("Failed to load config value for \"{}\" from \"{}\"", field.getName(), label, e);
                        }
                    }
                }

                clazz = clazz.getSuperclass();
            } while (deep && clazz != null);
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface ConfigField {
            String value() default "";          // The path to the value
            double min() default Double.NaN;    // For the numbers!
            double max() default Double.NaN;
        }
    }
}