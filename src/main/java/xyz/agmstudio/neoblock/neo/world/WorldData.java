package xyz.agmstudio.neoblock.neo.world;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.NeoListener;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldData extends MinecraftUtil.AbstractWorldData {
    private static WorldData instance;
    public static WorldData getInstance() {
        return instance;
    }

    public static final double AABB_RANGE = 1.05;
    public static final BlockPos POS = new BlockPos(0, 64, 0);
    public static final Vec3 POS_CORNER = new Vec3(POS.getX(), POS.getY(), POS.getZ());
    public static final BlockState DEFAULT_STATE = Blocks.GRASS_BLOCK.defaultBlockState();

    private static void resetTiers(WorldData data) {
        data.tiers.clear();
        TierData.stream().map(tier -> WorldTier.of(tier, data))
                .forEach(tier -> data.tiers.add(tier));

        instance.status.hash = TierData.getHash();
    }

    public static void setup(@NotNull ServerLevel level) {
        load(level);

        if (isInactive()) {
            boolean isNeoBlock = true;
            for (int y : List.of(-64, -61, 0, 64))
                if (!level.getBlockState(new BlockPos(0, y, 0)).isAir()) isNeoBlock = false;

            if (isNeoBlock) {
                level.setBlock(POS, DEFAULT_STATE, 3);
                UnmodifiableConfig rules = NeoBlockMod.getConfig().get("rules");
                if (rules != null) WorldRules.applyGameRules(level, rules);

                // Load schematics from config!
                Schematic.loadSchematic(level, POS, "main.nbt");
                int iterator = 0;
                while (NeoBlockMod.getConfig().contains("schematics.custom_" + iterator)) {
                    try {
                        UnmodifiableConfig scheme = NeoBlockMod.getConfig().get("schematics.custom_" + iterator);
                        String name = scheme.getOrElse("name", "NeoBlockSchematic_" + iterator);
                        BlockPos pos = new BlockPos(scheme.getInt("x"), scheme.getInt("y"), scheme.getInt("z"));
                        int result = Schematic.loadSchematic(level, pos, name);
                        if (result == 0) throw new FileNotFoundException("File \"" + name + "\" not found");
                    } catch (Exception e) {
                        NeoBlockMod.LOGGER.error("Unable to load schematic {}", iterator, e);
                    }
                    iterator++;
                }
                setActive();
            } else {
                NeoBlockMod.LOGGER.info("NeoBlock has been disabled.");
                MinecraftUtil.Messenger.sendMessage("message.neoblock.disabled_world_1", level, false);
                MinecraftUtil.Messenger.sendMessage("message.neoblock.disabled_world_2", level, false);
                setActive();
            }
        } else if (isUpdated()) {
            NeoBlockMod.LOGGER.info("NeoBlock tiers has been updated.");
            Component command = Component.literal("/neoblock force update").withStyle(
                    Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/neoblock force update"))
            );
            MinecraftUtil.Messenger.sendMessage("message.neoblock.updated_world", level, false, command);
            setActive();
        }
    }

    public static @NotNull WorldData create(@NotNull ServerLevel level) {
        WorldData data = new WorldData(level);

        data.status = new WorldStatus();
        resetTiers(data);

        NeoBlockMod.LOGGER.debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldData load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldData data = new WorldData(level);

        NeoBlockMod.LOGGER.debug("Loading WorldData from {}", tag);
        data.status = NBTSaveable.load(WorldStatus.class, tag);
        if (!isValid()) {
            data.status.state = WorldState.UPDATED;
            return data;
        }

        final ListTag tiers = tag.getList("Tiers", StringTag.TAG_COMPOUND);
        for (int i = 0; i < tiers.size(); i++) {
            WorldTier tier = NBTSaveable.load(WorldTier.class, tiers.getCompound(i));
            data.tiers.add(tier);
        }

        return data;
    }

    @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.merge(status.save());
        if (status.state == WorldState.ACTIVE) {
            ListTag list = new ListTag();
            for (WorldTier tier: tiers) list.add(tier.save());
            tag.put("Tiers", list);
        }

        NeoBlockMod.LOGGER.debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;

    private WorldStatus status;
    private HashSet<WorldTier> tiers;
    private final WorldUpgrade upgrade = new WorldUpgrade();

    private WorldData(ServerLevel level) {
        instance = this;
        this.level = level;
    }
    public WorldStatus getStatus() {
        return status;
    }
    public ServerLevel getLevel() {
        return level;
    }
    public WorldTier getTier(int id) {
        for (WorldTier tier: tiers) if (tier.getID() == id) return tier;
        return null;
    }

    public static HashSet<WorldTier> getTiers() {
        return instance.tiers;
    }

    public static boolean isInactive() {
        return instance.status.state == WorldState.INACTIVE;
    }
    public static void setActive() {
        instance.status.state = WorldState.ACTIVE;
        instance.setDirty();
    }
    public static boolean isActive() {
        return instance.status.state == WorldState.ACTIVE;
    }
    public void setDisabled() {
        instance.status.state = WorldState.DISABLED;
        instance.setDirty();
    }
    public static boolean isDisabled() {
        return instance.status.state == WorldState.DISABLED;
    }
    public static void setUpdated() {
        instance.status.state = WorldState.UPDATED;
        instance.setDirty();
    }
    public static boolean isUpdated() {
        return instance.status.state == WorldState.UPDATED;
    }

    public static int getBlockCount() {
        return instance.status.blockCount;
    }
    public static void setBlockCount(int count) {
        instance.status.blockCount = count;
        instance.setDirty();
    }
    public static void addBlockCount(int count) {
        instance.status.blockCount += count;
        instance.setDirty();
    }

    public static long getGameTime() {
        return instance.level.getGameTime();
    }

    public static int getTraderFailedAttempts() {
        return instance.status.traderFailedAttempts;
    }
    public static void resetTraderFailedAttempts() {
        instance.status.traderFailedAttempts = 0;
        instance.setDirty();
    }
    public static void addTraderFailedAttempts() {
        instance.status.traderFailedAttempts += 1;
        instance.setDirty();
    }

    public static HashMap<EntityType<?>, Integer> getTradedMobs() {
        return instance.status.tradedMobs;
    }
    public static void addTradedMob(EntityType<?> entityType, int count) {
        instance.status.tradedMobs.merge(entityType, count, Integer::sum);
        instance.setDirty();
    }
    public static void clearTradedMobs() {
        instance.status.tradedMobs.clear();
        instance.setDirty();
    }

    public static boolean isValid() {
        return Objects.equals(instance.status.hash, TierData.getHash());
    }
    public static void updateTiers() {
        resetTiers(instance);
    }

    public static @NotNull RandomSource getRandom() {
        return instance.level.getRandom();
    }

    public static void setCommanded(WorldTier tier, boolean force) {
        tier.lock.commanded = true;

        if (force && tier.canBeUnlocked()) instance.upgrade.addUpgrade(tier);
    }

    public static void tick(ServerLevel level, LevelAccessor access) {
        if (instance != null) instance.upgrade.tick(level, access);
    }

    public static BlockState getRandomBlock() {
        AtomicInteger totalChance = new AtomicInteger();
        List<WorldTier> tiers = new ArrayList<>();

        instance.tiers.stream().filter(WorldTier::isEnabled).forEach(tier -> {
            tiers.add(tier);
            totalChance.addAndGet(tier.getWeight());
        });

        if (totalChance.get() == 0) return DEFAULT_STATE;
        int randomValue = getRandom().nextInt(totalChance.get());
        for (WorldTier tier : tiers) {
            randomValue -= tier.getWeight();
            if (randomValue < 0) return tier.getRandomBlock();
        }

        NeoBlockMod.LOGGER.error("Unable to find a block for {} blocks", getBlockCount());
        return DEFAULT_STATE;
    }

    public static void setNeoBlock(@NotNull LevelAccessor access, BlockState block) {
        access.setBlock(POS, block, 3);

        Vec3 center = POS.getCenter();
        for(Entity entity: access.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }

    public static void onBlockBroken(ServerLevel level, LevelAccessor access, boolean triggered) {
        if (triggered) addBlockCount(1);
        for (WorldTier tier: instance.tiers) if (tier.canBeUnlocked())
            instance.upgrade.addUpgrade(tier);

        else setNeoBlock(access, getRandomBlock());

        Animation.resetIdleTick();
        NeoListener.execute(() -> NeoMerchant.attemptSpawnTrader(level));
    }

    public static boolean isOnUpgrade() {
        return !instance.upgrade.upgrades.isEmpty();
    }
}
