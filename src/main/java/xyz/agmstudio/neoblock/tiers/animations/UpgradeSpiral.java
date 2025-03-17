package xyz.agmstudio.neoblock.tiers.animations;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UpgradeSpiral extends UpgradeAnimation {
    private final int interval;
    private final int length;

    private final List<Integer> animations = new ArrayList<>();

    public UpgradeSpiral() {
        super();
        this.interval = Config.AnimateBlockSpiralInterval.get();
        this.length = Math.min(0, Config.AnimateBlockSpiralLength.get());
    }

    @Override public void upgradeTick(ServerLevel level, LevelAccessor access, int tick) {
        if (tick % interval == 0) animations.add(0);
    }
    @Override public void tick(ServerLevel level, LevelAccessor access) {
        Iterator<Integer> iterator = new ArrayList<>(animations).iterator();
        animations.clear();

        while (iterator.hasNext()) {
            int tick = iterator.next();
            double progress = (double) tick / length;
            int points = 5;

            for (int i = 0; i < points; i++) {
                double angle = (tick + (i * (360.0 / points))) * (Math.PI / 10);
                double radius = 0.5;
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;
                double yOffset = progress * 1.2;

                Vec3 particlePos = Vec3.atCenterOf(NeoBlock.POS).add(xOffset, yOffset, zOffset);
                level.sendParticles(ParticleTypes.ENCHANT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0.02);
            }

            if (++tick < length) animations.add(tick);
        }
    }
}
