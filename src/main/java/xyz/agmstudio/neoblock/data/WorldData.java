package xyz.agmstudio.neoblock.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.tiers.NeoBlock;

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

        CompoundTag pbc = tag.getCompound("PlayerBlockCount");
        pbc.getAllKeys().forEach(key -> data.playerBlockCount.put(key, pbc.getInt(key)));

        return data;
    }
    private int worldState;
    private int blockCount;
    private int traderFailedAttempts;
    private int unlockedTiers;

    private final HashMap<String, Integer> playerBlockCount;

    public WorldData() {
        worldState = 0;
        blockCount = 0;
        traderFailedAttempts = 0;
        playerBlockCount = new HashMap<>();

        unlockedTiers = 0;
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
    public int getPlayerBlockCount(@NotNull Entity player) {
        return playerBlockCount.getOrDefault(player.getStringUUID(), 0);
    }
    public void setPlayerBlockCount(@NotNull Entity player, int count) {
        playerBlockCount.put(player.getStringUUID(), count);
        setDirty();
    }
    public void addPlayerBlockCount(@NotNull Entity player, int count) {
        playerBlockCount.compute(player.getStringUUID(), (k, v) -> v == null ? count : v + count);
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

    public int getUnlockedTiers() {
        return unlockedTiers;
    }
    public void setUnlockedTiers(int tiers) {
        unlockedTiers = tiers;
    }
    public void updateUnlockedTiers() {
        for (int i = 0; i < NeoBlock.TIERS.size(); i++) {
            if (NeoBlock.TIERS.get(i).getUnlock() > blockCount) break;
            unlockedTiers = i;
        }
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

        CompoundTag pbc = tag.getCompound("PlayerBlockCount");
        playerBlockCount.keySet().forEach(key -> pbc.putInt(key, playerBlockCount.get(key)));

        return tag;
    }
}
