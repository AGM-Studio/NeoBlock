package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;

import java.util.HashMap;

public class WorldStatus extends NBTSaveable {
    private final WorldData data;

    @NBTData("Hash") protected String hash = TierData.getHash();
    @NBTData("WorldState") protected State state = State.INACTIVE;
    @NBTData("BlockCount") protected int blockCount = 0;
    @NBTData("TraderFailedAttempts") protected int traderFailedAttempts = 0;
    @NBTData("NeoBlock") protected BlockPos pos = new BlockPos(0, 64, 0);

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

    public WorldStatus(WorldData data) {
        this.data = data;
    }

    public boolean isInactive() {
        return state == State.INACTIVE;
    }
    public boolean isActive() {
        return state == State.ACTIVE;
    }
    public boolean isDisabled() {
        return state == State.DISABLED;
    }
    public boolean isUpdated() {
        return state == State.UPDATED;
    }

    public void setInactive() {
        state = State.INACTIVE;
        data.setDirty();
    }
    public void setActive() {
        state = State.ACTIVE;
        data.setDirty();
    }
    public void setDisabled() {
        state = State.DISABLED;
        data.setDirty();
    }
    public void setUpdated() {
        state = State.UPDATED;
        data.setDirty();
    }

    public int getBlockCount() {
        return blockCount;
    }
    public void setBlockCount(int count) {
        blockCount = count;
        data.setDirty();
    }

    public void addBlockCount(int count) {
        blockCount += count;
        data.setDirty();
    }

    public int getTraderFailedAttempts() {
        return traderFailedAttempts;
    }
    public void resetTraderFailedAttempts() {
        traderFailedAttempts = 0;
        data.setDirty();
    }
    public int addTraderFailedAttempts() {
        traderFailedAttempts += 1;
        data.setDirty();

        return traderFailedAttempts;
    }

    public HashMap<EntityType<?>, Integer> getTradedMobs() {
        return tradedMobs;
    }
    public void addTradedMob(EntityType<?> entityType, int count) {
        tradedMobs.merge(entityType, count, Integer::sum);
        data.setDirty();
    }
    public void clearTradedMobs() {
        tradedMobs.clear();
        data.setDirty();
    }

    public enum State {
        INACTIVE(0),    // Default, before activation
        ACTIVE(1),      // NeoBlock is running
        DISABLED(2),    // NeoBlock is disabled
        UPDATED(3);     // NeoBlock configs has been updated / Incompatible HASH

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static State fromId(int id) {
            for (State state : values()) {
                if (state.id == id) return state;
            }
            return INACTIVE;
        }
    }
}
