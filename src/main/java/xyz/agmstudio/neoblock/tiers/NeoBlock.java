package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.animations.ProgressbarAnimation;
import xyz.agmstudio.neoblock.tiers.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.tiers.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NeoBlock {
    public static Upgrade UPGRADE = null;

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

    public static class Upgrade {
        private static final HashSet<UpgradeProgressAnimation> progressAnimations = new HashSet<>();
        private static final HashSet<UpgradePhaseAnimation> phaseAnimations = new HashSet<>();
        private static ProgressbarAnimation progressbar = null;

        public static void clearProgressAnimations() {
            progressAnimations.clear();
        }
        public static void addProgressAnimation(UpgradeProgressAnimation animation) {
            progressAnimations.add(animation);
        }
        public static void clearPhaseAnimations() {
            phaseAnimations.clear();
        }
        public static void addPhaseAnimation(UpgradePhaseAnimation animation) {
            phaseAnimations.add(animation);
        }

        protected int UPGRADE_TICKS = 0;
        protected int UPGRADE_GOAL = 0;

        protected Upgrade() {
            progressbar = new ProgressbarAnimation();
            if (!progressbar.isEnabled()) progressbar = null;
        }

        public boolean isOnUpgrade() {
            return UPGRADE_GOAL > 0;
        }

        public void tick(ServerLevel level, LevelAccessor access) {
            for (UpgradeProgressAnimation animation: progressAnimations) animation.tick(level, access);
            if (!isOnUpgrade()) return;
            if (++UPGRADE_TICKS >= UPGRADE_GOAL) finishUpgrade(level, access);
            else {
                if (progressbar != null) progressbar.update(UPGRADE_TICKS, UPGRADE_GOAL);
                for (UpgradeProgressAnimation animation: progressAnimations) animation.upgradeTick(level, access, UPGRADE_TICKS);
            }
            DATA.setDirty();
        }

        public void finishUpgrade(ServerLevel level, LevelAccessor access) {
            if (DATA == null) return;
            NeoTier tier = DATA.getTier().next();
            if (tier != null && tier.isUnlocked()) {
                tier.onFinishUpgrade(level);
                DATA.setTier(tier);
            }

            if (progressbar != null) progressbar.removeAllPlayers();
            for (UpgradePhaseAnimation animation: phaseAnimations)
                if (animation.isActiveOnUpgradeFinish()) animation.animate(level, access);

            setNeoBlock(access, getRandomBlock());

            UPGRADE_TICKS = 0;
            UPGRADE_GOAL = 0;
        }

        public void startUpgrade(ServerLevel level, LevelAccessor access, NeoTier tier) {
            if (tier == null || !tier.isUnlocked()) return;
            setNeoBlock(access, Blocks.BEDROCK.defaultBlockState());
            UPGRADE_GOAL += tier.UNLOCK_TIME;
            tier.onStartUpgrade(level);
            if (UPGRADE_GOAL == 0) finishUpgrade(level, access);
            else if (progressbar != null) level.players().forEach(progressbar::addPlayer);

            for (UpgradePhaseAnimation animation : phaseAnimations)
                if (animation.isActiveOnUpgradeStart()) animation.animate(level, access);
        }

        protected void configure(int goal, int tick) {
            UPGRADE_GOAL = goal;
            UPGRADE_TICKS = tick;
        }

        public void addPlayer(ServerPlayer player) {
            if (progressbar != null) progressbar.addPlayer(player);
        }
    }
}
