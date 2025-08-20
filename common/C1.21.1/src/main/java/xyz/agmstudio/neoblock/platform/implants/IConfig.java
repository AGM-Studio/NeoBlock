package xyz.agmstudio.neoblock.platform.implants;

import xyz.agmstudio.neoblock.platform.helpers.IConfigHelper;

import java.util.Map;

public interface IConfig {
    class Path {
        private final String[] paths;
        private Path(String... paths) {
            this.paths = paths;
        }

        public String[] getPaths() {
            return paths;
        }

        public static Path of(String... paths) {
            return new Path(paths);
        }
    }

    default IConfig getSection(Path path) {
        for (String p: path.getPaths()) {
            IConfig result = getSection(p);
            if (IConfigHelper.isINull(result)) continue;
            return result;
        }

        return null;
    }
    IConfig getSection(String path);

    default boolean contains(Path path) {
        for (String p: path.getPaths()) if (contains(p)) return true;
        return false;
    }
    boolean contains(String path);

    default <T> T get(Path path) {
        return get(path, null);
    }
    default  <T> T get(Path path, T defaultValue) {
        for (String p: path.getPaths()) {
            T result = get(p);
            if (IConfigHelper.isINull(result)) continue;
            return result;
        }

        return defaultValue;
    }
    <T> T get(String path);
    <T> T get(String path, T defaultValue);

    default int getInt(Path path) {
        return this.<Number> get(path).intValue();
    }
    default int getInt(Path path, int defaultValue) {
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
    Map<String, Object> valueMap();
}
