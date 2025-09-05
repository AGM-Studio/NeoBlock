package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;

public abstract class UpgradeProgressAnimation extends Animation {
    @ConfigField(min = 5)
    protected int interval = 40;

    public UpgradeProgressAnimation(String name) {
        super("upgrade", name);
    }
    public UpgradeProgressAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        TierManager.addProgressAnimation(this);
    }

    /**
     * Will tick while upgrading is active. The default will call animate on interval tick.
     *
     * @param level the level to play animation
     * @param tick  the tick upgrade is in
     */
    public void upgradeTick(ServerLevel level, long tick) {
        if (tick % interval == 0) animate(level);
    }
}
