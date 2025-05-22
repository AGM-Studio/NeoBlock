package xyz.agmstudio.neoblock.neo.world;

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

public class WorldUpgrade {
    final List<WorldTier.Lock> upgrades = new ArrayList<>();
    public void tick(ServerLevel level, LevelAccessor access) {
        if (upgrades.isEmpty()) return;
        WorldTier.Lock lock = upgrades.get(0);
        if (lock.tick++ == 0) {
            lock.tier.onStartUpgrade(level);

            WorldData.setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());

            if (progressbar != null) level.players().forEach(progressbar::addPlayer);
            for (UpgradePhaseAnimation animation : phaseAnimations)
                if (animation.isActiveOnUpgradeStart()) animation.animate(level, access);
        }
        if (lock.tick >= lock.goal) {
            lock.unlocked = true;
            lock.tier.onFinishUpgrade(level);

            upgrades.remove(0);
            upgrades.forEach(l -> l.line -= 1);
            if (upgrades.isEmpty()) {
                if (progressbar != null) progressbar.removeAllPlayers();
                for (UpgradePhaseAnimation animation : phaseAnimations)
                    if (animation.isActiveOnUpgradeFinish()) animation.animate(level, access);

                WorldData.setNeoBlock(access, WorldData.getRandomBlock());
            }
        } else {
            if (progressbar != null) progressbar.update(lock.tick, lock.goal);
            for (UpgradeProgressAnimation animation : progressAnimations)
                animation.upgradeTick(level, access, lock.tick);
        }

        WorldData.getInstance().setDirty();
    }

    public void addUpgrade(WorldTier tier) {
        upgrades.add(tier.lock);
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
