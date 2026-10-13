package xyz.agmstudio.neoblock.compatibility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.platform.IConfig;

import java.util.Optional;

public class PerPlayerBlock {
    public final boolean enabled;
    private final String group;
    private final int distancing;

    private final IntProvider x;
    private final IntProvider y;
    private final IntProvider z;

    private int count = 0;

    public PerPlayerBlock() {
        this.enabled = false;
        this.group = null;
        this.distancing = 0;

        this.x = null;
        this.y = null;
        this.z = null;
    }
    public PerPlayerBlock(@NotNull IConfig section) {
        enabled = section.get("enabled");
        group = section.get("group");
        distancing = section.get("distancing");

        x = section.getIntProvider("x");
        y = section.getIntProvider("y");
        z = section.getIntProvider("z");
    }

    public void load(CompoundTag tag) {
        count = tag != null && tag.contains("count") ? tag.getInt("count") : 0;
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("count", count);
        return tag;
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (!enabled || !(player.level() instanceof ServerLevel level)) return;

        String bid = "ppb-" + player.getGameProfile().getId();
        Optional<NeoBlock> block = WorldManager.getBlocks().stream().filter(b -> b.getID().equals(bid)).findFirst();
        if (block.isEmpty()) {
            int[] d = getSpiralCoordinates(count++);
            BlockPos pos = new BlockPos(
                    d[0] * distancing + x.sample(level.random),
                    y.sample(level.random),
                    d[1] * distancing + z.sample(level.random)
            );
            NeoBlock.create(level, bid, pos, group);
            player.setRespawnPosition(level.dimension(), pos.above(), 0, true, false);

            Vec3 center = pos.above().getCenter();
            NeoMC.teleportEntity(player, level, center.x, center.y, center.z, 0, 0);
        }
    }
    public static int[] getSpiralCoordinates(int n) {
        if (n == 0) return new int[]{0, 0};

        int k = (int) Math.ceil((Math.sqrt(n + 1) - 1) / 2);
        int m = (2 * k + 1) * (2 * k + 1) - 1;
        int d = m - n;
        int x, y;

        if (d < 2 * k) {
            x = k - d;
            y = -k;
        } else if (d < 4 * k) {
            x = -k;
            y = -k + d - 2 * k;
        } else if (d < 6 * k) {
            x = -k + d - 4 * k;
            y = k;
        } else {
            x = k;
            y = k - d + 6 * k;
        }

        return new int[] {x, y};
    }
}