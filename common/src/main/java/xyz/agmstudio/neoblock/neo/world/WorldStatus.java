package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class WorldStatus implements NBTSaveable {
    private final WorldData data;

    @NBTData("WorldState") protected State state = State.INACTIVE;
    @NBTData("BlockCount") protected int blockCount = 0;
    @NBTData("TraderFailedAttempts") protected int traderFailedAttempts = 0;
    @NBTData("NeoBlock") protected BlockPos pos = new BlockPos(0, 64, 0);
    @NBTData("Dimension") protected String dimension = "minecraft:overworld";

    protected final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();
    protected final List<NeoBlockSpec> queue = new ArrayList<>();

    @Override public void onLoad(CompoundTag tag) {
        final CompoundTag mobs = tag.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> tradedMobs.merge(MinecraftUtil.getEntityType(key).orElse(null), mobs.getInt(key), Integer::sum));

        queue.clear();
        final ListTag blocks = tag.getList("Queue", Tag.TAG_STRING);
        blocks.forEach(block -> NeoBlockSpec.parse(block.getAsString()).ifPresent(queue::add));
    }
    @Override public CompoundTag onSave(CompoundTag tag) {
        final CompoundTag mobs = new CompoundTag();
        tradedMobs.forEach((key, value) -> mobs.putInt(String.valueOf(MinecraftUtil.getEntityTypeResource(key)), value));
        tag.put("TradedMobs", mobs);

        final ListTag blocks = new ListTag();
        queue.forEach(block -> blocks.add(StringTag.valueOf(block.getID())));
        tag.put("Queue", blocks);

        return tag;
    }

    public WorldStatus(WorldData data) {
        this.data = data;
    }

    public boolean isCorrectDimension(@NotNull ServerLevel level) {
        return isCorrectDimension(level.dimension());
    }
    public boolean isCorrectDimension(@NotNull ResourceKey<Level> dimension) {
        return dimension.location().toString().equals(this.dimension);
    }
    public ServerLevel getDimension() {
        return getDimension(data.getLevel().getServer());
    }
    public ServerLevel getDimension(MinecraftServer server) {
        @NotNull ResourceLocation location = MinecraftUtil.parseResourceLocation(this.dimension);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
        return server.getLevel(dimension);
    }
    public BlockPos getBlockPos() {
        return pos;
    }
    public Optional<NeoBlockSpec> getNextInQueue() {
        if (queue.isEmpty()) return Optional.empty();
        else return Optional.of(queue.remove(0));
    }
    public void addToQueue(NeoBlockSpec spec) {
        queue.add(spec);
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

    public void setDimension(@NotNull ServerLevel level) {
        setDimension(level.dimension());
    }
    public void setDimension(@NotNull ResourceKey<Level> dimension) {
        this.dimension = dimension.location().toString();
        data.setDirty();
    }
    public void setBlockPos(BlockPos pos, ServerLevel level) {
        BlockManager.cleanBlock(level, this.pos);

        this.pos = pos;
        data.setDirty();

        level.setDefaultSpawnPos(this.pos, 0.0f);
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
