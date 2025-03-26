package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.NeoListener;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.random.RandomGenerator;

public class NeoBlock {
    public static final double AABB_RANGE = 1.05;
    public static final BlockPos POS = new BlockPos(0, 64, 0);
    public static final BlockState DEFAULT_STATE = Blocks.GRASS_BLOCK.defaultBlockState();

    protected static HashSet<String> hash = new HashSet<>();

    public static List<NeoTier> TIERS = new ArrayList<>();

    public static BlockState getRandomBlock() {
        int breaks = WorldData.getBlockCount();
        int totalChance = WorldData.getUnlocked().stream().mapToInt(NeoTier::getWeight).sum();
        int randomValue = RandomGenerator.getDefault().nextInt(totalChance);
        for (NeoTier tier: WorldData.getUnlocked()) {
            randomValue -= tier.getWeight();
            if (randomValue < 0) return tier.getRandomBlock();
        }

        NeoBlockMod.LOGGER.error("Unable to find a block for {} blocks", breaks);
        return DEFAULT_STATE;
    }

    public static void setNeoBlock(@NotNull LevelAccessor access, BlockState block) {
        access.setBlock(NeoBlock.POS, block, 3);

        Vec3 center = NeoBlock.POS.getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }

    public static void setupWorldData(@NotNull ServerLevel level) {
        WorldData.load(level);

        if (WorldData.isInactive()) {
            boolean isNeoBlock = true;
            for (int y : List.of(-64, -61, 0, 64))
                if (!level.getBlockState(new BlockPos(0, y, 0)).isAir()) isNeoBlock = false;

            if (isNeoBlock) {
                level.setBlock(NeoBlock.POS, NeoBlock.DEFAULT_STATE, 3);
                WorldData.setActive();
            } else {
                NeoBlockMod.LOGGER.info("NeoBlock has been disabled.");
                MessagingUtil.sendMessage("message.neoblock.disabled_world_1", level, false);
                MessagingUtil.sendMessage("message.neoblock.disabled_world_2", level, false);
                WorldData.setActive();
            }
        } else if (WorldData.isUpdated()) {
            NeoBlockMod.LOGGER.info("NeoBlock tiers has been updated.");
            Component command = Component.literal("/neoblock force update").withStyle(
                    Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/neoblock force update"))
            );
            MessagingUtil.sendMessage("message.neoblock.updated_world", level, false, command);
            WorldData.setActive();
        }
    }

    public static void onBlockBroken(ServerLevel level, LevelAccessor access, boolean triggered) {
        if (triggered) WorldData.addBlockCount(1);
        for (NeoTier tier: TIERS) if (tier.canBeUnlocked())
            WorldData.getUpgradeManager().startUpgrade(level, access, tier);

        else setNeoBlock(access, getRandomBlock());

        NeoListener.execute(() -> NeoMerchant.attemptSpawnTrader(level));
    }

    public static void reload() {
        ResourceUtil.loadAllTierConfigs();

        int i = 0;
        NeoBlock.TIERS.clear();
        while (Files.exists(NeoTier.FOLDER.resolve("tier-" + i + ".toml")))
            NeoBlock.TIERS.add(new NeoTier(i++));

        hash.clear();
        NeoBlock.TIERS.stream().map(NeoTier::getHashCode).forEach(hash::add);

        NeoBlockMod.LOGGER.info("Loaded {} tiers from the tiers folder.", NeoBlock.TIERS.size());
    }

    public static boolean isOnUpgrade() {
        return WorldData.getUpgradeManager().isOnUpgrade();
    }
}
