package xyz.agmstudio.neoblock.tiers.animations.phase;


import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.animations.Animation;

import java.util.HashSet;

public abstract class UpgradePhaseAnimation extends Animation {
    @AnimationConfig("at-start")
    private boolean activeOnUpgradeStart = false;
    @AnimationConfig("at-finish")
    private boolean activeOnUpgradeFinish = true;

    public UpgradePhaseAnimation(String name) {
        this("phase", name);
    }
    public UpgradePhaseAnimation(String category, String name) {
        super(category, name);
        this.enabled = activeOnUpgradeStart || activeOnUpgradeFinish;
    }

    public boolean isActiveOnUpgradeFinish() {
        return activeOnUpgradeFinish;
    }
    public boolean isActiveOnUpgradeStart() {
        return activeOnUpgradeStart;
    }

    private static final HashSet<Class<? extends UpgradePhaseAnimation>> animations = new HashSet<>();
    public static void addAnimation(Class<? extends UpgradePhaseAnimation> clazz) {
        animations.add(clazz);
    }
    public static @NotNull HashSet<UpgradePhaseAnimation> getAnimations() {
        HashSet<UpgradePhaseAnimation> animations = new HashSet<>();
        for (Class<? extends UpgradePhaseAnimation> animation: UpgradePhaseAnimation.animations) {
            try {
                animations.add(animation.getConstructor().newInstance());
            } catch (Exception e) {
                NeoBlockMod.LOGGER.error("Error while instantiating {}", animation, e);
            }
        }
        return animations;
    }
}
