package xyz.agmstudio.neoblock.animations.progress;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.UpgradeManager;
import xyz.agmstudio.neoblock.animations.Animation;

import java.util.HashSet;

public abstract class UpgradeProgressAnimation extends Animation {
    public UpgradeProgressAnimation(String name) {
        super("upgrade", name);
    }
    public UpgradeProgressAnimation(String category, String name) {
        super(category, name);
    }

    @Override protected void onRegister() {
        UpgradeManager.addProgressAnimation(this);
    }

    /**
     * Will tick while upgrading is active. The default will call animate on interval tick.
     *
     * @param level  the level to play animation
     * @param access the world access if needed
     * @param tick   the tick upgrade is in
     */
    public abstract void upgradeTick(ServerLevel level, LevelAccessor access, int tick);

    private static final HashSet<Class<? extends UpgradeProgressAnimation>> animations = new HashSet<>();
    public static void addAnimation(Class<? extends UpgradeProgressAnimation> clazz) {
        if (!Animation.canRegisterNewAnimations()) return;
        animations.add(clazz);
    }
    public static @NotNull HashSet<UpgradeProgressAnimation> getAnimations() {
        HashSet<UpgradeProgressAnimation> animations = new HashSet<>();
        for (Class<? extends UpgradeProgressAnimation> animation: UpgradeProgressAnimation.animations) {
            try {
                animations.add(animation.getConstructor().newInstance());
            } catch (Exception e) {
                NeoBlockMod.LOGGER.error("Error while instantiating {}", animation, e);
            }
        }
        return animations;
    }
}
