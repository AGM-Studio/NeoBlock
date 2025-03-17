package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

public abstract class UpgradeAnimation extends Animation {
    public UpgradeAnimation() {
        super();
    }

    /**
     * Will tick while upgrading is active
     *
     * @param level the level to play animation
     * @param access the world access if needed
     * @param tick the tick upgrade is in
     */
    public void upgradeTick(ServerLevel level, LevelAccessor access, int tick) {}
}
