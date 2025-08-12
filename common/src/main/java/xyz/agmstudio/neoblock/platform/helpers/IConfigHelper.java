package xyz.agmstudio.neoblock.platform.helpers;

import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.implants.IConfig;

import java.nio.file.Path;

public interface IConfigHelper {
    static IConfig getIConfig(Path path) {
        return Services.CONFIG.getConfig(path);
    }
    static boolean isINull(Object object) {
        return Services.CONFIG.isNull(object);
    }

    IConfig getConfig(Path path);
    boolean isNull(Object object);
}
