package xyz.agmstudio.neoblock.data;

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

    public static ModConfigSpec.ConfigValue<Float> NeoMerchantChance;
    public static ModConfigSpec.ConfigValue<Float> NeoMerchantChanceIncrement;
    public static ModConfigSpec.ConfigValue<Integer> NeoMerchantAttemptInterval;
    public static ModConfigSpec.ConfigValue<Integer> NeoMerchantLifespanMin;
    public static ModConfigSpec.ConfigValue<Integer> NeoMerchantLifespanMax;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateProgressbar;
    public static ModConfigSpec.ConfigValue<Boolean> AnimateProgressbarName;
    public static ModConfigSpec.ConfigValue<String> AnimateProgressbarColor;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateUpgradeExplosion;
    public static ModConfigSpec.ConfigValue<Float> AnimateUpgradeExplosionVolume;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateUpgradeFuse;
    public static ModConfigSpec.ConfigValue<Float> AnimateUpgradeFuseVolume;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateBlockBreaking;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockBreakingInterval;
    public static ModConfigSpec.ConfigValue<Float> AnimateBlockBreakingVolume;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateBlockSpiral;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockSpiralInterval;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockSpiralLength;

    public static ModConfigSpec.ConfigValue<Boolean> AnimateBlockSparkle;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockSparkleInterval;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockSparkleLength;
    public static ModConfigSpec.ConfigValue<Integer> AnimateBlockSparkleFactor;

    protected Config(ModConfigSpec.Builder builder) {
        builder.push("neo-trader");
        NeoMerchantChance = builder
                .comment("Chance of spawning a NeoMerchant per interval. If no trades are available or another trader lives, no NeoMerchant will be spawned.")
                .define("chance", 0.3f);
        NeoMerchantChanceIncrement = builder
                .comment("On a failed attempt, will increase the chance of NeoMerchant per interval by this amount.")
                .define("chance-increment", 0.075f);
        NeoMerchantAttemptInterval = builder
                .comment("How many blocks should be broken to attempt to spawn a NeoMerchant.")
                .define("attempt-interval", 10);
        NeoMerchantLifespanMin = builder
                .comment("The minimum ticks the trader should exist before despawning.")
                .define("life-span-min", 20 * 60 * 20);
        NeoMerchantLifespanMax = builder
                .comment("The maximum ticks the trader should exist before despawning.")
                .define("life-span-max", 20 * 60 * 40);
        builder.pop();

        builder.push("Animations");
        builder.push("Progressbar");
        AnimateProgressbar = builder
                .comment("If you want the progressbar shows up while upgrading the NeoBlock set this to true, otherwise set it to false")
                .define("enabled", true);
        AnimateProgressbarName = builder
                .define("show-time", true);
        AnimateProgressbarColor = builder
                .comment("Supported colors are `red`, `green`, `blue`, `pink`, `yellow`, `purple`, `white`")
                .define("color", "red");
        builder.pop();

        builder.push("Upgrade Start");
        builder.push("Fuse");
        AnimateUpgradeFuse = builder
                .comment("If the fuse should be played when upgrade is started.")
                .define("enabled", true);
        AnimateUpgradeFuseVolume = builder
                .comment("The volume of the block breaking sound.")
                .define("volume", 0.7f);
        builder.pop();
        builder.pop();

        builder.push("Upgrade Finish");
        builder.push("Explosion");
        AnimateUpgradeExplosion = builder
                .comment("If the explosion with the thumb should be played when upgrade is finished.")
                .define("enabled", true);
        AnimateUpgradeExplosionVolume = builder
                .comment("The volume of the block breaking sound.")
                .define("volume", 0.7f);
        builder.pop();
        builder.pop();

        builder.push("Block");
        builder.push("Breaking-Animation");
        AnimateBlockBreaking = builder
                .comment("This animation will show block breaking particles and sound every second.")
                .define("enabled", true);
        AnimateBlockBreakingInterval = builder
                .comment("Interval time in ticks. Each 20 ticks is one second.")
                .define("interval", 20);
        AnimateBlockBreakingVolume = builder
                .comment("The volume of the block breaking sound.")
                .define("volume", 0.7f);
        builder.pop();

        builder.push("Spiral-Animation");
        AnimateBlockSpiral = builder
                .comment("This animation will show enchanting particles.")
                .define("enabled", false);
        AnimateBlockSpiralInterval = builder
                .comment("Interval time in ticks.")
                .define("interval", 50);
        AnimateBlockSpiralLength = builder
                .comment("The length of animation in ticks. If bigger number is used, multiple spirals might appear.")
                .define("length", 20);
        builder.pop();

        builder.push("Sparkle");
        AnimateBlockSparkle = builder
                .comment("This animation will show sparkling particles once a while")
                .define("enabled", true);
        AnimateBlockSparkleInterval = builder
                .comment("Interval time in ticks.")
                .define("interval", 200);
        AnimateBlockSparkleLength = builder
                .comment("The length of animation in ticks. It's recommended to use lower numbers.")
                .define("length", 20);
        AnimateBlockSparkleFactor = builder
                .comment("The spam protection factor as an Integer.")
                .define("block-breaking", 3);
        builder.pop();
        builder.pop();
        builder.pop();
    }
}