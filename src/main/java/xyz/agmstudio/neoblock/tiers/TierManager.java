package xyz.agmstudio.neoblock.tiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TierManager {
    public static List<NeoTier> TIERS = new ArrayList<>();

    public static void reload() {
        ResourceUtil.loadAllTierConfigs();

        int i = 0;
        TIERS.clear();
        while (Files.exists(NeoTier.FOLDER.resolve("tier-" + i + ".toml")))
            TIERS.add(new NeoTier(i++));

        NeoBlock.hash.clear();
        TIERS.stream().map(NeoTier::getHashCode).forEach(NeoBlock.hash::add);

        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", TIERS.size());
    }


    // Animations
    private static final HashSet<UpgradeProgressAnimation> progressAnimations = new HashSet<>();
    private static final HashSet<UpgradePhaseAnimation> phaseAnimations = new HashSet<>();
    private static ProgressbarAnimation progressbar = null;

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

    public static List<Animation> getAllAnimations() {
        List<Animation> list = new ArrayList<>();
        list.addAll(progressAnimations);
        list.addAll(phaseAnimations);
        return list;
    }

    // Upgrades
    private final List<Upgrade> upgrades = new ArrayList<>();

    public boolean isOnUpgrade() {
        return !upgrades.isEmpty();
    }

    public void tick(ServerLevel level, LevelAccessor access) {
        if (upgrades.isEmpty()) return;
        Upgrade upgrade = upgrades.get(0);
        if (upgrade.tick()) {
            upgrades.remove(0);
            finishUpgrade(level, access, upgrade);
        } else {
            if (progressbar != null) progressbar.update(upgrade.tick, upgrade.goal);
            for (UpgradeProgressAnimation animation : progressAnimations)
                animation.upgradeTick(level, access, upgrade.tick);
        }
        // Some animations might need to be finished
        for (UpgradeProgressAnimation animation : progressAnimations) animation.tick(level, access);
        WorldData.getInstance().setDirty();
    }

    public void startUpgrade(ServerLevel level, LevelAccessor access, @NotNull NeoTier tier) {
        Upgrade upgrade = new Upgrade(tier);
        upgrades.add(upgrade);

        NeoBlock.setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());
        tier.onStartUpgrade(level);

        if (progressbar != null) level.players().forEach(progressbar::addPlayer);
        for (UpgradePhaseAnimation animation : phaseAnimations)
            if (animation.isActiveOnUpgradeStart()) animation.animate(level, access);
    }
    private void finishUpgrade(ServerLevel level, LevelAccessor access, @NotNull Upgrade upgrade) {
        WorldData.unlockTier(upgrade.tier);
        upgrade.tier.onFinishUpgrade(level);

        if (!upgrades.isEmpty()) return;
        if (progressbar != null) progressbar.removeAllPlayers();
        for (UpgradePhaseAnimation animation : phaseAnimations)
            if (animation.isActiveOnUpgradeFinish()) animation.animate(level, access);

        NeoBlock.setNeoBlock(access, NeoBlock.getRandomBlock());
    }

    public void addPlayer(ServerPlayer player) {
        if (progressbar != null) progressbar.addPlayer(player);
    }

    private static class Upgrade {
        private final NeoTier tier;
        private final int goal;

        private int tick = 0;

        protected Upgrade(@NotNull NeoTier tier) {
            this.tier = tier;
            this.goal = tier.lock.getTime();
        }
        private Upgrade(@NotNull NeoTier tier, int tick) {
            this.tier = tier;
            this.goal = tier.lock.getTime();
            this.tick = tick;
        }

        public static Upgrade fromTag(CompoundTag tag) {
            try {
                NeoTier tier = TIERS.get(tag.getInt("Tier"));
                return new Upgrade(tier, tag.getInt("Tick"));
            } catch (Exception ignored) {}
            return null;
        }

        protected boolean tick() {
            return ++tick >= goal;
        }

        public CompoundTag getTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Tier", tier.id);
            tag.putInt("Tick", tick);
            return tag;
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "{tier=" + tier + ", goal=" + goal + ", tick=" + tick + '}';
        }
    }

    // Data managements
    public void save(@NotNull CompoundTag tag) {
        ListTag upgrades = new ListTag();
        for (Upgrade upgrade: this.upgrades) upgrades.add(upgrade.getTag());
        tag.put("Upgrades", upgrades);
    }
    public void load(@NotNull ListTag tag) {
        for (int i = 0; i < tag.size(); i++) {
            Upgrade upgrade = Upgrade.fromTag(tag.getCompound(i));
            NeoBlockMod.LOGGER.debug("Loading upgrade from {} and got {}", tag.getCompound(i), upgrade);
            if (upgrade != null) upgrades.add(upgrade);
        }
    }
}
