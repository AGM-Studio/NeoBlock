package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.ScoreboardUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class NeoBlock {
    public static final BlockPos POS = new BlockPos(0, 64, 0);
    public static BlockScoreboard BLOCK_SCOREBOARD = null;
    public static WorldScoreboard WORLD_SCOREBOARD = null;

    public static List<NeoTier> TIERS = new ArrayList<>();

    public static BlockState getRandomBlock() {
        int breaks = BLOCK_SCOREBOARD.getScore("Total");
        List<NeoTier> availableTiers = TIERS.stream().filter(tier -> tier.getUnlock() <= breaks).toList();
        int totalChance = availableTiers.stream().mapToInt(NeoTier::getWeight).sum();

        AtomicInteger remainingChance = new AtomicInteger(RandomGenerator.getDefault().nextInt(totalChance));
        int tier = IntStream.range(0, availableTiers.size()).filter(i -> (remainingChance.addAndGet(-availableTiers.get(i).getWeight()) < 0)).findFirst().orElse(0);
        List<BlockState> blocks = availableTiers.get(tier).getBlocks();

        int rnd = RandomGenerator.getDefault().nextInt(blocks.size());
        return blocks.get(rnd);
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof Level level) || level.dimension() != Level.OVERWORLD || level.isClientSide) return;
        if (BLOCK_SCOREBOARD != null) NeoBlockMod.LOGGER.warn("NeoBlock.BLOCK_SCOREBOARD is already loaded.");
        if (WORLD_SCOREBOARD != null) NeoBlockMod.LOGGER.warn("NeoBlock.WORLD_SCOREBOARD is already loaded.");
        BLOCK_SCOREBOARD = ScoreboardUtil.of(event.getLevel(), BlockScoreboard.class);
        WORLD_SCOREBOARD = ScoreboardUtil.of(event.getLevel(), WorldScoreboard.class);

        NeoBlockMod.LOGGER.debug("Loaded {} tiers.", NeoBlock.TIERS.size());

        if (WORLD_SCOREBOARD.isActive() || WORLD_SCOREBOARD.isDormant()) return;
        boolean isNeoBlock = true;
        for (int y: List.of(-64, -61, 0, 64))
            if (!level.getBlockState(new BlockPos(0, y, 0)).isAir()) isNeoBlock = false;

        if (isNeoBlock) {
            level.setBlock(NeoBlock.POS, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            WORLD_SCOREBOARD.setActive();
        } else {
            NeoBlockMod.LOGGER.info("NeoBlock has set to dormant.");
            MessagingUtil.sendMessage("message.neoblock.dormant_world_1", level);
            MessagingUtil.sendMessage("message.neoblock.dormant_world_2", level);
            WORLD_SCOREBOARD.setDormant();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level) || level.dimension() != Level.OVERWORLD) return;
        BLOCK_SCOREBOARD = null;
        WORLD_SCOREBOARD = null;
    }

    @SubscribeEvent
    public void onWorldTick(LevelTickEvent.Post event) {
        final LevelAccessor access = event.getLevel();
        if (access instanceof Level level && level.dimension() != Level.OVERWORLD || access.isClientSide()) return;
        if (BLOCK_SCOREBOARD == null || WORLD_SCOREBOARD == null || WORLD_SCOREBOARD.isDormant()) return;

        BlockState block = access.getBlockState(NeoBlock.POS);
        if (WORLD_SCOREBOARD.isActive() && (block.isAir() || block.canBeReplaced())) {
            BLOCK_SCOREBOARD.addScore(null, 1);

            access.setBlock(NeoBlock.POS, getRandomBlock(), 3);

            Vec3 center = NeoBlock.POS.getCenter();
            for(Entity entity: access.getEntities(null, AABB.ofSize(center, 1.2, 1.2, 1.2)))
                entity.teleportTo(center.x, center.y + 0.55, center.z);

            TIERS.forEach(tier -> tier.checkScore((Level) access));
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getPos().equals(NeoBlock.POS)) return;
        BLOCK_SCOREBOARD.addScore(event.getPlayer(), 1);
    }

    public static class BlockScoreboard extends ScoreboardUtil {
        public BlockScoreboard(Scoreboard scoreboard) {
            super(scoreboard, "NeoBlockBroken");
        }
        public void addScore(@Nullable ScoreHolder holder, int value) {
            if (holder == null) holder = getStoragePlayer("Total");
            setScore(holder, value + getScore(holder));
        }
    }
    public static class WorldScoreboard extends ScoreboardUtil {
        public WorldScoreboard(Scoreboard scoreboard) {
            super(scoreboard, "NeoWorldData");
        }
        public void setActive() {
            setScore("Setup", 1);
        }
        public boolean isActive() {
            return getScore("Setup") == 1;
        }
        public void setDormant() {
            setScore("Setup", 2);
        }
        public boolean isDormant() {
            return getScore("Setup") == 2;
        }
    }
}
