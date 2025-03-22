package xyz.agmstudio.neoblock.tiers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class WorldData extends SavedData {
    private static final String DATA_NAME = "custom_data";

    public static WorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(WorldData::new, WorldData::load), DATA_NAME);
    }
    public static WorldData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        WorldData data = new WorldData();
        data.worldState = tag.getInt("WorldState");
        data.blockCount = tag.getInt("BlockCount");
        data.traderFailedAttempts = tag.getInt("TraderFailedAttempts");

        data.updateTier();
        CompoundTag upgrade = tag.getCompound("Upgrade");
        data.upgrade.configure(
                tag.getInt("Goal"),
                tag.getInt("Tick")
        );

        return data;
    }
    private int worldState;
    private int blockCount;
    private int traderFailedAttempts;
    private NeoTier tier = null;

    private final NeoBlock.Upgrade upgrade = new NeoBlock.Upgrade();
    private final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();

    public WorldData() {
        worldState = 0;
        blockCount = 0;
        traderFailedAttempts = 0;
    }

    public void setActive() {
        worldState = 1;
        setDirty();
    }
    public boolean isActive() {
        return worldState == 1;
    }
    public void setDormant() {
        worldState = 2;
        setDirty();
    }
    public boolean isDormant() {
        return worldState == 2;
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

    public @NotNull NeoTier getTier() {
        if (tier == null) updateTier();
        return tier == null ? NeoBlock.TIERS.getFirst() : tier;
    }
    public void setTier(NeoTier tier) {
        this.tier = tier;
    }
    public void updateTier() {
        this.tier = NeoBlock.TIERS.stream()
                .takeWhile(tier -> tier.getUnlock() <= this.blockCount)  // Can't use isUnlocked
                .reduce((first, second) -> first.TIER > second.TIER ? first : second)
                .orElse(NeoBlock.TIERS.getFirst());
    }

    @Override
    public String toString() {
        return "NeoWorldData[blockCount=" + blockCount + ", worldState=" + worldState + "]";
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("WorldState", worldState);
        tag.putInt("BlockCount", blockCount);
        tag.putInt("TraderFailedAttempts", traderFailedAttempts);

        final CompoundTag upgrade = tag.getCompound("Upgrade");
        upgrade.putInt("Goal", this.upgrade.UPGRADE_GOAL);
        upgrade.putInt("Tick", this.upgrade.UPGRADE_TICKS);
        tag.put("Upgrade", upgrade);

        final CompoundTag mobs = tag.getCompound("TradedMobs");
        tradedMobs.forEach((key, value) -> mobs.putInt(BuiltInRegistries.ENTITY_TYPE.getKey(key).toString(), value));
        tag.put("TradedMobs", mobs);

        return tag;
    }

    public NeoBlock.Upgrade fetchUpgrade() {
        return upgrade;
    }
    public HashMap<EntityType<?>, Integer> getTradedMobs() {
        return tradedMobs;
    }
    public void addTradedMob(EntityType<?> entityType, int count) {
        tradedMobs.merge(entityType, count, Integer::sum);
        setDirty();
    }
}
