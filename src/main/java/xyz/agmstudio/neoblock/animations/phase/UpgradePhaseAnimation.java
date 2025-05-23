package xyz.agmstudio.neoblock.animations.phase;


import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.world.WorldUpgrade;

import java.util.HashSet;

public abstract class UpgradePhaseAnimation extends Animation {
    public UpgradePhaseAnimation(String name) {
        super("phase", name);
    }
    public UpgradePhaseAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        WorldUpgrade.addPhaseAnimation(this);
    }

    public abstract boolean isActiveOnUpgradeFinish();
    public abstract boolean isActiveOnUpgradeStart();

    private static final HashSet<Class<? extends UpgradePhaseAnimation>> animations = new HashSet<>();
    public static void addAnimation(Class<? extends UpgradePhaseAnimation> clazz) {
        if (!Animation.canRegisterNewAnimations()) return;
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
