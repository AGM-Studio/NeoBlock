package xyz.agmstudio.neoblock.platform;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import xyz.agmstudio.neoblock.platform.implants.IConfig;

import java.util.Map;

public final class ForgeConfig implements IConfig {
    private final UnmodifiableConfig config;

    public ForgeConfig(UnmodifiableConfig config) {
        this.config = config;

        if (config instanceof FileConfig c) c.load();
    }

    @Override public IConfig getSection(String path) {
        UnmodifiableConfig config = this.config.get(path);
        return new ForgeConfig(config);
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

    @Override public Map<String, Object> valueMap() {
        return config.valueMap();
    }
}
