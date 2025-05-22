package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.TierManager;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class WorldData extends MinecraftUtil.AbstractWorldData {
    private static WorldData instance;
    public static WorldData getInstance() {
        return instance;
    }

    private static void resetTiers(WorldData data) {
        data.tiers.clear();
        TierData.stream().map(tier -> WorldTier.of(tier, data))
                .forEach(tier -> data.tiers.add(tier));

        instance.status.hash = TierData.getHash();
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

    private final TierManager tierManager = new TierManager();

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

    public static TierManager getTierManager() {
        return instance.tierManager;
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

    public static void tick(ServerLevel level, LevelAccessor access) {
        if (instance != null) instance.tierManager.tick(level, access);
    }
}
