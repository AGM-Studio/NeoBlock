package xyz.agmstudio.neoblock.platform;

import com.electronwill.nightconfig.core.NullObject;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import xyz.agmstudio.neoblock.platform.helpers.IConfigHelper;
import xyz.agmstudio.neoblock.platform.implants.IConfig;

import java.nio.file.Path;

public class NeoForgeConfigHelper implements IConfigHelper {
    @Override public IConfig getConfig(Path path) {
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        return new NeoForgeConfig(config);
    }

    @Override public boolean isNull(Object object) {
        return object == null || NullObject.NULL_OBJECT == object;
    }
}
