package xyz.agmstudio.neoblock.animations.progress;

import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;

public abstract class CooldownProgressAnimation extends Animation {
    @ConfigField(min = 5)
    protected int interval = 40;

    public CooldownProgressAnimation(String name) {
        super("upgrade", name);
    }
    public CooldownProgressAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        Animation.addProgressAnimation(this);
    }

    /**
     * Will tick while upgrading is active. The default will call animate on interval tick.
     *
     * @param block the NeoBlock to play animation for
     * @param tick  the tick upgrade is in
     */
    public void upgradeTick(NeoBlock block, long tick) {
        if (tick % interval == 0) animate(block);
    }
}