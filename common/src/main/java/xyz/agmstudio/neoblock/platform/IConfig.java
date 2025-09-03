package xyz.agmstudio.neoblock.platform;

import xyz.agmstudio.neoblock.NeoBlock;

import java.util.Map;

public interface IConfig {
    Helper HELPER = NeoBlock.loadService(Helper.class);
    interface Helper {
        IConfig getConfig(java.nio.file.Path path);
        boolean isNull(Object object);
    }

    static IConfig getIConfig(java.nio.file.Path path) {
        return HELPER.getConfig(path);
    }
    static boolean isINull(Object object) {
        return HELPER.isNull(object);
    }

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
            if (isINull(result)) continue;
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
            if (isINull(result)) continue;
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
