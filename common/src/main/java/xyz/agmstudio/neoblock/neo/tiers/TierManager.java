package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TierManager {
    private static final List<TierResearch> researches = new ArrayList<>();

    public static List<TierSpec> fetchTiers(boolean loadConfig) {
        ResourceUtil.loadAllTierConfigs();

        List<TierSpec> tiers = new ArrayList<>();
        for (int i = 0; Files.exists(TierSpec.FOLDER.resolve("tier-" + i + ".toml")); i++)
            tiers.add(new TierSpec(i, loadConfig));

        NeoBlock.LOGGER.info("Loaded {} tiers from the tiers folder.", tiers.size());
        return tiers;
    }

    public static void loadTierConfig(final TierSpec spec) {
        Path FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID, "tiers");
        IConfig config = IConfig.getConfig(FOLDER, "tier-" + spec.id);
        if (config == null) throw new NBTSaveable.AbortException("Unable to find config for tier " + spec.id);

        NeoBlock.LOGGER.debug("Loading tier {}...", spec.id);
        spec.name = config.get("name", "Tier-" + spec.id);

        spec.requirements.clear();
        spec.research.time = config.getInt("unlock.unlock-time", 0);
        if (spec.id > 0) {
            long time = config.getInt("unlock.game-time", -1);
            if (time > 0) spec.requirements.add(new TierRequirement.GameTime(time));
            long blocks = config.getInt("unlock.blocks", -1);
            if (blocks > 0) spec.requirements.add(new TierRequirement.BlockBroken(blocks));
            if (config.get("unlock.command", spec.requirements.isEmpty()))
                spec.requirements.add(new TierRequirement.Special());
        } else {
            spec.research.done = true;
        }

        spec.blocks.clear();
        final List<String> blocks_list = config.get("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(spec.blocks::add));

        if (spec.blocks.isEmpty()) spec.weight = 0;
        else spec.weight = Math.max(0, config.getInt("weight", 1));

        final List<String> list = config.get("trader-trades", config.get("trades", List.of()));
        spec.trades = NeoTradePool.parse(list);

        final List<NeoBlockSpec> start = NeoSeqBlockSpec.extractSequenceList(config.get("starting-blocks", List.of()));
        spec.startSequence = new NeoSeqBlockSpec(start, 1, "tier-" + spec.id + "-start");

        spec.unlockActions = new TierSpecActions(config, "on-unlock").withMessage("message.neoblock.unlocking_trader", spec.id);
        spec.enableActions = new TierSpecActions(config, "on-enable").withMessage("message.neoblock.enabling_trader", spec.id);
        spec.disableActions = new TierSpecActions(config, "on-disable").withMessage("message.neoblock.disabling_trader", spec.id);
        spec.researchActions = new TierSpecActions(config, "on-research").withMessage("message.neoblock.research_trader", spec.id);

        NeoBlock.LOGGER.debug("Tier {} loaded. Hash key: {}", spec.id, spec.getHashCode());
    }

    public static void tick(ServerLevel level) {
        if (researches.isEmpty()) return;
        TierResearch research = researches.get(0);
        if (research.tick++ == 0) {
            research.onStart(level);

            BlockManager.BEDROCK_SPEC.placeAt(level, NeoBlockPos.get());

            if (TierManager.progressbar != null) level.players().forEach(TierManager.progressbar::addPlayer);
            for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                if (animation.isActiveOnUpgradeStart()) animation.animate(level);
        }
        if (research.isTimeDone()) {
            research.done = true;
            research.onFinish(level);

            researches.remove(0);
            if (researches.isEmpty()) {
                if (TierManager.progressbar != null) TierManager.progressbar.removeAllPlayers();
                for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                    if (animation.isActiveOnUpgradeFinish()) animation.animate(level);

                BlockManager.getRandomBlock().placeAt(level, NeoBlockPos.get());
            }
        } else {
            if (TierManager.progressbar != null) TierManager.progressbar.update(research.tick, research.time);
            for (UpgradeProgressAnimation animation : TierManager.progressAnimations)
                animation.upgradeTick(level, research.tick);
        }

        WorldData.getInstance().setDirty();
    }

    public static @Nullable TierResearch fetchCurrentResearch() {
        if (researches.isEmpty()) return null;
        return researches.get(0);
    }
    public static void addResearch(TierResearch research) {
        researches.add(research);
    }
    public static boolean hasResearch() {
        return !researches.isEmpty();
    }
    public static void reloadResearches() {
        researches.clear();
        WorldData.getWorldTiers().forEach(tier -> {
            if (tier.canBeResearched()) tier.startResearch();
        });
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
