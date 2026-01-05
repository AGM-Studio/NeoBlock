package xyz.agmstudio.neoblock.animations.phase;


import xyz.agmstudio.neoblock.animations.Animation;

public abstract class CooldownPhaseAnimation extends Animation {
    public CooldownPhaseAnimation(String name) {
        super("phase", name);
    }
    public CooldownPhaseAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        Animation.addPhaseAnimation(this);
    }

    public abstract boolean isActiveOnUpgradeFinish();
    public abstract boolean isActiveOnUpgradeStart();
}
