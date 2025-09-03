package xyz.agmstudio.neoblock.platform;

import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.platform.helpers.IConfigHelper;
import xyz.agmstudio.neoblock.platform.helpers.INBTHelper;

import java.util.ServiceLoader;

public class Services {
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);
    public static final INBTHelper NBT = load(INBTHelper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        NeoBlock.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}