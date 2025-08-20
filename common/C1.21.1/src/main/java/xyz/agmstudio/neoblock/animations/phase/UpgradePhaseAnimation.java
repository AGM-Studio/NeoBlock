package xyz.agmstudio.neoblock.animations.phase;


import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;

public abstract class UpgradePhaseAnimation extends Animation {
    public UpgradePhaseAnimation(String name) {
        super("phase", name);
    }
    public UpgradePhaseAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        TierManager.addPhaseAnimation(this);
    }

    public abstract boolean isActiveOnUpgradeFinish();
    public abstract boolean isActiveOnUpgradeStart();
}
