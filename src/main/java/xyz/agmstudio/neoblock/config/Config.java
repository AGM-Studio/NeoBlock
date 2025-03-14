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

    public static ModConfigSpec.ConfigValue<Float> NeoTraderChance;
    public static ModConfigSpec.ConfigValue<Float> NeoTraderChanceIncrement;
    public static ModConfigSpec.ConfigValue<Integer> NeoTraderAttemptInterval;
    public static ModConfigSpec.ConfigValue<Integer> NeoTraderLifespanMin;
    public static ModConfigSpec.ConfigValue<Integer> NeoTraderLifespanMax;

    protected Config(ModConfigSpec.Builder builder) {
        builder.push("neo-trader");
        NeoTraderChance = builder
                .comment("Chance of spawning a NeoTrader per interval. If no trades are available or another trader lives, no NeoTrader will be spawned.")
                .define("chance", 0.3f);
        NeoTraderChanceIncrement = builder
                .comment("On a failed attempt, will increase the chance of NeoTrader per interval by this amount.")
                .define("chance-increment", 0.075f);
        NeoTraderAttemptInterval = builder
                .comment("How many blocks should be broken to attempt to spawn a NeoTrader.")
                .define("attempt-interval", 10);
        NeoTraderLifespanMin = builder
                .comment("The minimum ticks the trader should exists before despawning.")
                .define("life-span-min", 20 * 60 * 20);
        NeoTraderLifespanMax = builder
                .comment("The maximum ticks the trader should exists before despawning.")
                .define("life-span-max", 20 * 60 * 40);

        builder.pop();
    }
}