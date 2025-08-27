package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
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
    public static final double AABB_RANGE = 1.0;

    public static BlockPos getBlockPos() {
        return WorldData.getWorldStatus().getBlockPos();
    }
    public static Vec3 getBlockCorner() {
        BlockPos pos = getBlockPos();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static NeoBlockSpec getRandomBlock() {
        Optional<NeoBlockSpec> queued = WorldData.getWorldStatus().getNextInQueue();
        if (queued.isPresent()) return queued.get();

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

        NeoBlock.LOGGER.error("Unable to find a block for {} blocks", WorldData.getWorldStatus().getBlockCount());
        return DEFAULT_SPEC;
    }

    public static void updateBlock(ServerLevel level, boolean trigger) {
        if (!TierManager.hasResearch()) getRandomBlock().placeAt(level, getBlockPos());
        else BEDROCK_SPEC.placeAt(level, getBlockPos());  // Creative cheaters & Move block in mid-search (Just in case)

        if (!trigger) return;
        Animation.resetIdleTick();
        WorldData.getWorldStatus().addBlockCount(1);
        NeoBlock.execute(() -> NeoMerchant.attemptSpawnTrader(level));

        for (TierSpec tier: WorldData.getWorldTiers())
            if (tier.canBeResearched()) tier.startResearch();
    }

    public static void ensureNoFall(@NotNull LevelAccessor access) {
        Vec3 center = getBlockPos().getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }

    public static BlockState getCurrentBlock(ServerLevel level) {
        return level.getBlockState(getBlockPos());
    }

    public static void cleanBlock(ServerLevel level, BlockPos pos) {
        BlockState block = BlockManager.getCurrentBlock(level);
        if (block.getBlock().equals(Blocks.BEDROCK))
            BlockManager.DEFAULT_SPEC.placeAt(level, pos);
    }

    public static void tick(ServerLevel level) {
        final BlockState block = level.getBlockState(getBlockPos());
        if (WorldData.getWorldStatus().isUpdated() || TierManager.hasResearch()) {
            if (block.getBlock() != Blocks.BEDROCK) BEDROCK_SPEC.placeAt(level, getBlockPos());
        } else if (block.isAir() || block.canBeReplaced()) updateBlock(level, true);
    }

    public static boolean isNeoBlock(BlockPos pos) {
        return getBlockPos().equals(pos);
    }
}
