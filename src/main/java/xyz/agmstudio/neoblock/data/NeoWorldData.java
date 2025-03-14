package xyz.agmstudio.neoblock.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class NeoWorldData extends SavedData {
    private static final String DATA_NAME = "custom_data";
    public static NeoWorldData get(Level level) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return null;
        return server.getDataStorage().computeIfAbsent(new Factory<>(NeoWorldData::new, NeoWorldData::load), DATA_NAME);
    }
    public static NeoWorldData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        NeoWorldData data = new NeoWorldData();
        data.worldState = tag.getInt("WorldState");
        data.blockCount = tag.getInt("BlockCount");

        CompoundTag pbc = tag.getCompound("PlayerBlockCount");
        pbc.getAllKeys().forEach(key -> data.playerBlockCount.put(key, pbc.getInt(key)));

        return data;
    }
    private int worldState;
    private int blockCount;

    private final HashMap<String, Integer> playerBlockCount;

    public NeoWorldData() {
        worldState = 0;
        blockCount = 0;
        playerBlockCount = new HashMap<>();
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

    @Override
    public String toString() {
        return "NeoWorldData[blockCount=" + blockCount + ", worldState=" + worldState + "]";
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("WorldState", worldState);
        tag.putInt("BlockCount", blockCount);

        CompoundTag pbc = tag.getCompound("PlayerBlockCount");
        playerBlockCount.keySet().forEach(key -> pbc.putInt(key, playerBlockCount.get(key)));

        return tag;
    }
}
