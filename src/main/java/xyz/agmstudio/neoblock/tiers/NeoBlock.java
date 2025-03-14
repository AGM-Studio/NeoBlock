package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
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
import xyz.agmstudio.neoblock.config.Config;
import xyz.agmstudio.neoblock.data.NeoWorldData;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.Range;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

@EventBusSubscriber(modid = NeoBlockMod.MOD_ID)
public class NeoBlock {
    public static final BlockPos POS = new BlockPos(0, 64, 0);
    public static BlockState DEFAULT_STATE = Blocks.GRASS_BLOCK.defaultBlockState();

    public static List<NeoTier> TIERS = new ArrayList<>();
    public static NeoWorldData DATA = null;

    public static BlockState getRandomBlock() {
        int breaks = DATA.getBlockCount();
        List<NeoTier> availableTiers = TIERS.stream().filter(tier -> tier.getUnlock() <= breaks).toList();
        if (availableTiers.isEmpty()) {
            NeoBlockMod.LOGGER.error("No available tiers for {} blocks", breaks);
            return DEFAULT_STATE;
        }

        int totalChance = availableTiers.stream().mapToInt(NeoTier::getWeight).sum();
        int randomValue = RandomGenerator.getDefault().nextInt(totalChance);
        for (NeoTier tier: availableTiers) {
            randomValue -= tier.getWeight();
            if (randomValue < 0) return tier.getRandomBlock();
        }

        NeoBlockMod.LOGGER.error("Unable to find a block for {} blocks", breaks);
        return DEFAULT_STATE;
    }

    public static void regenerateNeoBlock(ServerLevel level, LevelAccessor access, boolean score) {
        if (score) DATA.addBlockCount(1);

        access.setBlock(NeoBlock.POS, getRandomBlock(), 3);

        Vec3 center = NeoBlock.POS.getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, 1.2, 1.2, 1.2)))
            entity.teleportTo(center.x, center.y + 0.55, center.z);

        TIERS.forEach(tier -> tier.checkScore(level));
    }
    public static void attemptSpawnTrader(ServerLevel level) {
        int breaks = DATA.getBlockCount();
        if (breaks % NeoTrader.attemptInterval != 0 || NeoTrader.exists(level, "NeoTrader")) return;
        float chance = NeoTrader.chance + (NeoTrader.increment * DATA.getTraderFailedAttempts());
        if (RandomGenerator.getDefault().nextFloat() > chance) {
            DATA.addTraderFailedAttempts();
            NeoBlockMod.LOGGER.debug("Trader chance {} failed for {} times in a row", chance, DATA.getTraderFailedAttempts());
            return;
        }
        DATA.resetTraderFailedAttempts();
        List<NeoTrade> trades = new ArrayList<>();
        TIERS.stream().filter(tier -> tier.getUnlock() <= breaks)
                .forEach(tier -> trades.addAll(tier.getRandomTrades()));

        if (!trades.isEmpty()) {
            Villager trader = NeoTrader.spawnTraderWith(trades, level);
            MessagingUtil.sendInstantMessage("message.neoblock.trader_spawned", level, true);
        }
    }

    public static void reload() {
        int i = 0;
        NeoBlock.TIERS.clear();
        while (Files.exists(NeoTier.FOLDER.resolve("tier-" + i + ".toml")))
            NeoBlock.TIERS.add(new NeoTier(i++));

        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", NeoBlock.TIERS.size());

        NeoTrader.chance = Config.NeoTraderChance.get();
        NeoTrader.increment = Config.NeoTraderChanceIncrement.get();
        NeoTrader.attemptInterval = Config.NeoTraderAttemptInterval.get();
        NeoTrader.lifespan = new Range(Config.NeoTraderLifespanMin.get(), Config.NeoTraderLifespanMax.get());
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
            level.setBlock(NeoBlock.POS, DEFAULT_STATE, 3);
            DATA.setActive();
        } else {
            NeoBlockMod.LOGGER.info("NeoBlock has set to dormant.");
            MessagingUtil.sendMessage("message.neoblock.dormant_world_1", level, false);
            MessagingUtil.sendMessage("message.neoblock.dormant_world_2", level, false);
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
            regenerateNeoBlock(level, access, true);
            attemptSpawnTrader(level);
        }
        NeoTrader.manageTraders(level);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getPos().equals(NeoBlock.POS) || event.getLevel().isClientSide()) return;
        DATA.addPlayerBlockCount(event.getPlayer(), 1);
    }
}
