package xyz.agmstudio.neoblock.platform.implants;

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

    <T> T get(Path path);
    <T> T get(Path path, T defaultValue);
    <T> T get(String path);
    <T> T get(String path, T defaultValue);

    void load();
}
