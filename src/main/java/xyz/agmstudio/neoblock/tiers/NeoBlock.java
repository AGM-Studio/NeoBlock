package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NeoBlock {
    public static NeoBlockUpgrade UPGRADE = null;

    public static final double AABB_RANGE = 1.05;
    public static final BlockPos POS = new BlockPos(0, 64, 0);
    public static BlockState DEFAULT_STATE = Blocks.GRASS_BLOCK.defaultBlockState();

    public static List<NeoTier> TIERS = new ArrayList<>();
    public static WorldData DATA = null;

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

    public static void setNeoBlock(LevelAccessor access, BlockState block) {
        access.setBlock(NeoBlock.POS, block, 3);

        Vec3 center = NeoBlock.POS.getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }

    public static void reload() {
        if (!Files.exists(NeoTier.FOLDER)) {
            for (int i = 0; i < 10; i++) try {
                NeoTier.loadFromResources(i, false);
            } catch (Exception ignored) { break; }
            NeoTier.loadFromResources("template", true);
        }

        int i = 0;
        NeoBlock.TIERS.clear();
        while (Files.exists(NeoTier.FOLDER.resolve("tier-" + i + ".toml")))
            NeoBlock.TIERS.add(new NeoTier(i++));

        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", NeoBlock.TIERS.size());
    }

    public static void setupWorldData(ServerLevel level) {
        DATA = Optional.ofNullable(level).map(WorldData::get).orElse(null);
        if (DATA == null || DATA.isActive() || DATA.isDormant()) {
            if (DATA != null) DATA.setDirty();
            return;
        }
        NeoBlock.UPGRADE = DATA.fetchUpgrade();

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

    public static void onBlockBroken(ServerLevel level, LevelAccessor access, boolean triggered) {
        if (triggered) DATA.addBlockCount(1);
        NeoTier next = DATA.getTier().next();
        if (next != null && next.isUnlocked()) UPGRADE.startUpgrade(level, access, next);
        else setNeoBlock(access, getRandomBlock());
    }
}
