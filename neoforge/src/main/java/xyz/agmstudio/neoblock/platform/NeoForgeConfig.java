package xyz.agmstudio.neoblock.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import xyz.agmstudio.neoblock.platform.helpers.IConfigHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;

public class NeoForgeConfig implements IConfig {
    private final CommentedFileConfig config;

    protected NeoForgeConfig(CommentedFileConfig config) {
        this.config = config;
    }


    @Override public <T> T get(Path path) {
        for (String p: path.getPaths()) {
            T result = get(p);
            if (IConfigHelper.isINull(result)) continue;
            return result;
        }

        return null;
    }

    @Override
    public <T> T get(Path path, T defaultValue) {
        T result = get(path);
        return IConfigHelper.isINull(result) ? defaultValue : result;
    }

    @Override
    public <T> T get(String path) {
        return config.get(path);
    }

    @Override
    public <T> T get(String path, T defaultValue) {
        return config.getOrElse(path, defaultValue);
    }

    @Override
    public void load() {
        config.load();
    }
}
