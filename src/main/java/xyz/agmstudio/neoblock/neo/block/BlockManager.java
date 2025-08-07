package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.NeoListener;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockManager {
    public static final NeoBlockSpec DEFAULT_SPEC = new NeoBlockSpec(Blocks.GRASS_BLOCK);
    public static final NeoBlockSpec BEDROCK_SPEC = new NeoBlockSpec(Blocks.BEDROCK);
    public static final double AABB_RANGE = 1.02;

    public static BlockPos getBlockPos() {
        return WorldData.getWorldStatus().getBlockPos();
    }
    public static Vec3 getBlockCorner() {
        BlockPos pos = getBlockPos();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static NeoBlockSpec getRandomBlock() {
        AtomicInteger totalChance = new AtomicInteger();
        List<TierSpec> tiers = new ArrayList<>();

        WorldData.getWorldTiers().stream().filter(TierSpec::isEnabled).forEach(tier -> {
            tiers.add(tier);
            totalChance.addAndGet(tier.getWeight());
        });

        if (totalChance.get() == 0) return DEFAULT_SPEC;
        int randomValue = WorldData.getRandom().nextInt(totalChance.get());
        for (TierSpec tier : tiers) {
            randomValue -= tier.getWeight();
            if (randomValue < 0) return tier.getRandomBlock();
        }

        NeoBlockMod.LOGGER.error("Unable to find a block for {} blocks", WorldData.getWorldStatus().getBlockCount());
        return DEFAULT_SPEC;
    }

    public static void onBlockBroken(ServerLevel level, LevelAccessor access) {
        Animation.resetIdleTick();
        WorldData.getWorldStatus().addBlockCount(1);

        for (TierSpec tier: WorldData.getWorldTiers())
            if (tier.canBeResearched()) tier.startResearch();

        if (!TierManager.hasResearch()) getRandomBlock().placeAt(access, getBlockPos());
        else BEDROCK_SPEC.placeAt(access, getBlockPos());  // Creative cheaters (Just in case)

        NeoListener.execute(() -> NeoMerchant.attemptSpawnTrader(level));
    }

    public static void ensureNoFall(@NotNull LevelAccessor access) {
        Vec3 center = getBlockPos().getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }
}
