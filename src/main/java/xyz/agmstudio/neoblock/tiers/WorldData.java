package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.util.HashMap;
import java.util.HashSet;

public class WorldData extends SavedData {
    private static final String DATA_NAME = "custom_data";

    public static @NotNull WorldData get(@NotNull ServerLevel level) {
        WorldData data = level.getDataStorage().computeIfAbsent(new Factory<>(WorldData::create, WorldData::load), DATA_NAME);
        data.level = level;
        return data;
    }
    public static @NotNull WorldData create() {
        WorldData data = new WorldData();

        for (NeoTier tier: NeoBlock.TIERS) {
            data.encoding.add(tier.getHashCode());
            if (tier.TIER == 0 || tier.isUnlocked(data))
                data.unlocked.add(tier);
        }
        NeoBlock.TIERS.stream().map(NeoTier::getHashCode).forEach(data.encoding::add);

        NeoBlockMod.LOGGER.debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldData load(@NotNull CompoundTag tag, HolderLookup.Provider lookupProvider) {
        WorldData data = new WorldData();
        data.state = WorldState.fromId(tag.getInt("WorldState"));
        data.blockCount = tag.getInt("BlockCount");
        data.traderFailedAttempts = tag.getInt("TraderFailedAttempts");

        CompoundTag upgrade = tag.getCompound("Upgrade");
        data.upgrade.configure(
                upgrade.getInt("Goal"),
                upgrade.getInt("Tick")
        );

        CompoundTag mobs = upgrade.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> data.tradedMobs.merge(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(key)), mobs.getInt(key), Integer::sum));

        ListTag hash = tag.getList("Hashes", StringTag.TAG_STRING);
        for (int i = 0; i < hash.size(); ++i) data.encoding.add(hash.getString(i));

        NeoBlockMod.LOGGER.debug("Loaded WorldData from {}", tag);

        if (!data.isValid()) {
            data.setUpdated();
            NeoBlockMod.LOGGER.warn("Tiers has already been updated. NeoTier will be disabled till (/neoblock force update) is executed.");
        } else {
            ListTag unlocked = tag.getList("Unlocked", StringTag.TAG_INT);
            for (int i = 0; i < unlocked.size(); ++i) data.unlocked.add(NeoBlock.TIERS.get(unlocked.getInt(i)));
        }

        return data;
    }
    @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("WorldState", state.getId());
        tag.putInt("BlockCount", blockCount);
        tag.putInt("TraderFailedAttempts", traderFailedAttempts);

        final CompoundTag upgrade = tag.getCompound("Upgrade");
        upgrade.putInt("Goal", this.upgrade.UPGRADE_GOAL);
        upgrade.putInt("Tick", this.upgrade.UPGRADE_TICKS);
        tag.put("Upgrade", upgrade);

        final CompoundTag mobs = tag.getCompound("TradedMobs");
        tradedMobs.forEach((key, value) -> mobs.putInt(BuiltInRegistries.ENTITY_TYPE.getKey(key).toString(), value));
        tag.put("TradedMobs", mobs);

        final ListTag hash = new ListTag();
        encoding.stream().map(StringTag::valueOf).forEach(hash::add);
        tag.put("Encoding", hash);

        final ListTag utg = new ListTag();
        unlocked.forEach(tier -> utg.add(IntTag.valueOf(tier.TIER)));
        tag.put("Unlocked", utg);

        NeoBlockMod.LOGGER.debug("Saving WorldData to {}", tag);
        return tag;
    }


    private ServerLevel level;
    private WorldState state;
    private int blockCount;
    private int traderFailedAttempts;

    private final HashSet<NeoTier> unlocked = new HashSet<>();
    private final HashSet<String> encoding = new HashSet<>();

    private final UpgradeManager upgrade = new UpgradeManager();
    private final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();

    public WorldData() {
        state = WorldState.INACTIVE;
        blockCount = 0;
        traderFailedAttempts = 0;
    }

    public boolean isInactive() {
        return state == WorldState.INACTIVE;
    }
    public void setActive() {
        state = WorldState.ACTIVE;
        setDirty();
    }
    public boolean isActive() {
        return state == WorldState.ACTIVE;
    }
    public void setDisabled() {
        state = WorldState.DISABLED;
        setDirty();
    }
    public boolean isDisabled() {
        return state == WorldState.DISABLED;
    }
    public void setUpdated() {
        state = WorldState.UPDATED;
        setDirty();
    }
    public boolean isUpdated() {
        return state == WorldState.UPDATED;
    }

    public int getBlockCount() {
        return blockCount;
    }
    public void setBlockCount(int count) {
        blockCount = count;
        setDirty();
    }
    public void addBlockCount(int count) {
        blockCount += count;
        setDirty();
    }

    public int getTraderFailedAttempts() {
        return traderFailedAttempts;
    }
    public void resetTraderFailedAttempts() {
        traderFailedAttempts = 0;
        setDirty();
    }
    public void addTraderFailedAttempts() {
        traderFailedAttempts += 1;
        setDirty();
    }

    public HashSet<NeoTier> getUnlocked() {
        return unlocked;
    }

    @Override public String toString() {
        return "NeoWorldData[blockCount=" + blockCount + ", worldState=" + state + "]";
    }

    public UpgradeManager getUpgradeManager() {
        return upgrade;
    }
    public HashMap<EntityType<?>, Integer> getTradedMobs() {
        return tradedMobs;
    }
    public void addTradedMob(EntityType<?> entityType, int count) {
        tradedMobs.merge(entityType, count, Integer::sum);
        setDirty();
    }
    public void clearTradedMobs() {
        tradedMobs.clear();
        setDirty();
    }

    public boolean isValid() {
        HashSet<String> current = new HashSet<>();
        NeoBlock.TIERS.stream().map(NeoTier::getHashCode).forEach(current::add);

        return encoding.equals(current);
    }
    public boolean updateTiers() {
        // todo unlocked tiers

        encoding.clear();
        NeoBlock.TIERS.stream().map(NeoTier::getHashCode).forEach(encoding::add);

        return true;
    }
}
