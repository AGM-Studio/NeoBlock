package xyz.agmstudio.neoblock.neo.tiers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TierManager {
    private static final List<TierResearch> researches = new ArrayList<>();

    public static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");

    protected static void loadConfig(final TierSpec spec) {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "tier-" + spec.id);
        if (config == null) throw new NBTSaveable.AbortException("Unable to find config for tier " + spec.id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", spec.id);
        spec.name = config.getOrElse("name", "Tier-" + spec.id);

        spec.requirements.clear();
        spec.research.time = config.getIntOrElse("unlock.unlock-time", 0);
        if (spec.id > 0) {
            long time = config.getIntOrElse("unlock.game-time", -1);
            if (time > 0) spec.requirements.add(new TierRequirement.GameTime(time));
            long blocks = config.getIntOrElse("unlock.blocks", -1);
            if (blocks > 0) spec.requirements.add(new TierRequirement.BlockBroken(blocks));
            if (config.getOrElse("unlock.command", spec.requirements.isEmpty()))
                spec.requirements.add(new TierRequirement.Special());
        } else {
            spec.research.done = true;
        }

        spec.blocks.clear();
        final List<String> blocks_list = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(spec.blocks::add));

        if (spec.blocks.isEmpty()) spec.weight = 0;
        else spec.weight = Math.max(0, config.getIntOrElse("weight", 1));

        final List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        spec.tradePoolUnlock = NeoTradePool.parse(unlockTrades);

        final List<String> list = config.getOrElse("trader-trades", config.getOrElse("trades", List.of()));
        spec.trades = NeoTradePool.parse(list);

        final List<NeoBlockSpec> start = NeoSeqBlockSpec.extractSequenceList(config.getOrElse("starting-blocks", List.of()));
        spec.startSequence = new NeoSeqBlockSpec(start, 1, "tier-" + spec.id + "-start");

        NeoBlockMod.LOGGER.debug("Tier {} loaded. Hash key: {}", spec.id, spec.getHashCode());
    }

    public static void tick(ServerLevel level, LevelAccessor access) {
        if (researches.isEmpty()) return;
        TierResearch research = researches.get(0);
        if (research.tick++ == 0) {
            research.onStart(level);

            BlockManager.BEDROCK_SPEC.placeAt(level, BlockManager.getBlockPos());

            if (TierManager.progressbar != null) level.players().forEach(TierManager.progressbar::addPlayer);
            for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                if (animation.isActiveOnUpgradeStart()) animation.animate(level, access);
        }
        if (research.isTimeDone()) {
            research.done = true;
            research.onFinish(level);

            researches.remove(0);
            if (researches.isEmpty()) {
                if (TierManager.progressbar != null) TierManager.progressbar.removeAllPlayers();
                for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                    if (animation.isActiveOnUpgradeFinish()) animation.animate(level, access);

                BlockManager.getRandomBlock().placeAt(access, BlockManager.getBlockPos());
            }
        } else {
            if (TierManager.progressbar != null) TierManager.progressbar.update(research.tick, research.time);
            for (UpgradeProgressAnimation animation : TierManager.progressAnimations)
                animation.upgradeTick(level, access, research.tick);
        }

        WorldData.getInstance().setDirty();
    }
    public static void addResearch(TierResearch research) {
        researches.add(research);
    }
    public static boolean hasResearch() {
        return !researches.isEmpty();
    }

    // Animations
    public static final HashSet<UpgradeProgressAnimation> progressAnimations = new HashSet<>();
    public static final HashSet<UpgradePhaseAnimation> phaseAnimations = new HashSet<>();
    public static ProgressbarAnimation progressbar = null;

    public static void clearProgressAnimations() {
        progressAnimations.clear();
    }
    public static void addProgressAnimation(UpgradeProgressAnimation animation) {
        progressAnimations.add(animation);
    }
    public static void clearPhaseAnimations() {
        phaseAnimations.clear();
    }
    public static void addPhaseAnimation(UpgradePhaseAnimation animation) {
        phaseAnimations.add(animation);
    }
    public static void reloadProgressbarAnimations() {
        progressbar = new ProgressbarAnimation();
        progressbar.reload();
        if (!progressbar.isEnabled()) progressbar = null;
    }
    public static @NotNull List<Animation> getAllAnimations() {
        List<Animation> list = new ArrayList<>();
        list.addAll(progressAnimations);
        list.addAll(phaseAnimations);
        return list;
    }
    public static void addPlayer(ServerPlayer player) {
        if (progressbar != null) progressbar.addPlayer(player);
    }
}
