package xyz.agmstudio.neoblock.tiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UpgradeManager {
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
        if (!progressbar.isEnabled()) progressbar = null;
    }

    public static List<Animation> getAllAnimations() {
        List<Animation> list = new ArrayList<>();
        list.addAll(progressAnimations);
        list.addAll(phaseAnimations);
        return list;
    }

    private final List<Upgrade> upgrades = new ArrayList<>();

    public boolean isOnUpgrade() {
        return !upgrades.isEmpty();
    }

    public void tick(ServerLevel level, LevelAccessor access) {
        if (upgrades.isEmpty()) return;
        Upgrade upgrade = upgrades.getFirst();
        if (upgrade.tick()) {
            upgrades.removeFirst();
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

    public void save(@NotNull CompoundTag tag) {
        ListTag upgrades = new ListTag();
        for (Upgrade upgrade: this.upgrades) upgrades.add(upgrade.getTag());
        tag.put("Upgrades", upgrades);
    }
    public void load(@NotNull ListTag tag) {
        for (int i = 0; i < tag.size(); i++) {
            Upgrade upgrade = Upgrade.fromTag(tag.getCompound(i));
            if (upgrade != null) upgrades.add(upgrade);
        }
    }

    private static class Upgrade {
        private final NeoTier tier;
        private final int goal;

        private int tick = 0;

        protected Upgrade(@NotNull NeoTier tier) {
            this.tier = tier;
            this.goal = tier.UNLOCK_TIME;
        }
        private Upgrade(@NotNull NeoTier tier, int tick) {
            this.tier = tier;
            this.goal = tier.UNLOCK_TIME;
            this.tick = tick;
        }

        public static Upgrade fromTag(CompoundTag tag) {
            try {
                NeoTier tier = NeoBlock.TIERS.get(tag.getInt("Tier"));
                return new Upgrade(tier, tag.getInt("Tick"));
            } catch (Exception ignored) {}
            return null;
        }

        protected boolean tick() {
            return ++tick >= goal;
        }

        public CompoundTag getTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Tier", tier.TIER);
            tag.putInt("Tick", tick);
            return tag;
        }
    }
}
