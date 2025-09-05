package xyz.agmstudio.neoblock.platform;

import com.electronwill.nightconfig.core.NullObject;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class NeoForgeConfig implements IConfig {
    private final UnmodifiableConfig config;

    protected NeoForgeConfig(UnmodifiableConfig config) {
        this.config = config;

        if (config instanceof FileConfig c) c.load();
    }

    @Override public IConfig getSection(String path) {
        UnmodifiableConfig config = this.config.get(path);
        return config != null ? new NeoForgeConfig(config) : null;
    }

    @Override public boolean contains(String path) {
        return config.contains(path);
    }

    @Override public <T> T get(String path) {
        return config.get(path);
    }
    @Override public <T> T get(String path, T defaultValue) {
        return config.getOrElse(path, defaultValue);
    }

    @Override public void load() {
        if (config instanceof FileConfig c) c.load();
    }

    @Override public Set<String> keys() {
        return config.entrySet().stream().map(UnmodifiableConfig.Entry::getKey).collect(Collectors.toSet());
    }
    @Override public void forEach(BiConsumer<String, Object> action) {
        config.entrySet().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    public static class Helper implements IConfig.Helper {
        @Override public IConfig getConfig(java.nio.file.Path path) {
            CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
            return new NeoForgeConfig(config);
        }

        @Override public boolean isNull(Object object) {
            return object == null || NullObject.NULL_OBJECT == object;
        }
    }
}
