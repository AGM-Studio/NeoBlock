package xyz.agmstudio.neoblock.config;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    static {
        final Pair<Config, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Config::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static final Path CONFIG_PATH = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID);
    public static final Config CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    protected Config(ModConfigSpec.Builder builder) {
    }
}