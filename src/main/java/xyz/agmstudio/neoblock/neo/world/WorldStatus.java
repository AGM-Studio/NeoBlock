package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;

import java.util.HashMap;

public class WorldStatus extends NBTSaveable {
    @NBTData("Hash") protected String hash = TierData.getHash();
    @NBTData("WorldState") protected WorldState state = WorldState.INACTIVE;
    @NBTData("BlockCount") protected int blockCount = 0;
    @NBTData("TraderFailedAttempts") protected int traderFailedAttempts = 0;

    protected final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();

    @Override public void onLoad(CompoundTag tag) {
        final CompoundTag mobs = tag.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> tradedMobs.merge(MinecraftAPI.getEntityType(key).orElse(null), mobs.getInt(key), Integer::sum));
    }
    @Override public CompoundTag onSave(CompoundTag tag) {
        final CompoundTag mobs = new CompoundTag();
        tradedMobs.forEach((key, value) -> mobs.putInt(String.valueOf(MinecraftAPI.getEntityTypeResource(key)), value));
        tag.put("TradedMobs", mobs);

        return tag;
    }

    public int getBlockCount() {
        return blockCount;
    }
}
