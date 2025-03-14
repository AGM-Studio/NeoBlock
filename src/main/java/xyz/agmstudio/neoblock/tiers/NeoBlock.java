package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NeoWorldData;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

@EventBusSubscriber(modid = NeoBlockMod.MOD_ID)
public class NeoBlock {
    public static final BlockPos POS = new BlockPos(0, 64, 0);

    public static List<NeoTier> TIERS = new ArrayList<>();
    public static NeoWorldData DATA = null;

    public static BlockState getRandomBlock() {
        int breaks = DATA.getBlockCount();
        List<NeoTier> availableTiers = TIERS.stream().filter(tier -> tier.getUnlock() <= breaks).toList();
        int totalChance = availableTiers.stream().mapToInt(NeoTier::getWeight).sum();

        AtomicInteger remainingChance = new AtomicInteger(RandomGenerator.getDefault().nextInt(totalChance));
        int tier = IntStream.range(0, availableTiers.size()).filter(i -> (remainingChance.addAndGet(-availableTiers.get(i).getWeight()) < 0)).findFirst().orElse(0);
        List<BlockState> blocks = availableTiers.get(tier).getBlocks();

        int rnd = RandomGenerator.getDefault().nextInt(blocks.size());
        return blocks.get(rnd);
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;

        DATA = NeoWorldData.get(level);
        if (DATA == null || DATA.isActive() || DATA.isDormant()) return;
        boolean isNeoBlock = true;
        for (int y: List.of(-64, -61, 0, 64))
            if (!level.getBlockState(new BlockPos(0, y, 0)).isAir()) isNeoBlock = false;

        if (isNeoBlock) {
            level.setBlock(NeoBlock.POS, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            DATA.setActive();
        } else {
            NeoBlockMod.LOGGER.info("NeoBlock has set to dormant.");
            MessagingUtil.sendMessage("message.neoblock.dormant_world_1", level);
            MessagingUtil.sendMessage("message.neoblock.dormant_world_2", level);
            DATA.setDormant();
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        DATA = null;
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        final LevelAccessor access = event.getLevel();
        BlockState block = access.getBlockState(NeoBlock.POS);
        if (block.isAir() || block.canBeReplaced()) {
            DATA.addBlockCount(1);

            access.setBlock(NeoBlock.POS, getRandomBlock(), 3);

            Vec3 center = NeoBlock.POS.getCenter();
            for(Entity entity: access.getEntities(null, AABB.ofSize(center, 1.2, 1.2, 1.2)))
                entity.teleportTo(center.x, center.y + 0.55, center.z);

            TIERS.forEach(tier -> tier.checkScore(level));
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getPos().equals(NeoBlock.POS) || event.getLevel().isClientSide()) return;
        DATA.addPlayerBlockCount(event.getPlayer(), 1);
    }
}
