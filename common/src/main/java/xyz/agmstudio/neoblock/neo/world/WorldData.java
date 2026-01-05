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
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.events.NeoEventAction;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.PatternUtil;

import java.util.*;
import java.util.regex.Matcher;

public class WorldData implements NBTSaveable {
    private final WorldManager data;

    @NBTData("WorldState") protected State state = State.INACTIVE;
    @NBTData("BlockCount") protected int blockCount = 0;
    @NBTData("LastTierSpawn") protected int lastTierSpawn = 0;
    @NBTData("TraderFailedAttempts") protected int traderFailedAttempts = 0;
    @NBTData("NeoBlock") protected BlockPos pos = new BlockPos(0, 64, 0);
    @NBTData("Dimension") protected String dimension = "minecraft:overworld";

    protected final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();
    protected final List<NeoBlockSpec> queue = new ArrayList<>();
    protected final List<WorldCooldown> cooldowns = new ArrayList<>();

    protected final LinkedHashMap<Integer, NeoEventAction> onBlockActions = new LinkedHashMap<>();
    protected final LinkedHashMap<Integer, NeoEventAction> everyBlockActions = new LinkedHashMap<>();

    @Override public void onLoad(CompoundTag tag) {
        final CompoundTag mobs = tag.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> tradedMobs.merge(MinecraftUtil.getEntityType(key).orElse(null), mobs.getInt(key), Integer::sum));

        queue.clear();
        final ListTag blocks = tag.getList("Queue", Tag.TAG_STRING);
        blocks.forEach(block -> NeoBlockSpec.parse(block.getAsString()).ifPresent(queue::add));

        cooldowns.clear();
        final ListTag cools = tag.getList("Cooldowns", Tag.TAG_COMPOUND);
        cools.forEach(cool -> {
            WorldCooldown cooldown = NBTSaveable.instance(WorldCooldown.class, (CompoundTag) cool);
            cooldowns.add(cooldown);
        });

        IConfig config = NeoBlock.getConfig();
        for (String key: config.keys()) {
            Matcher obm = PatternUtil.ON_BLOCK_PATTERN.matcher(key);
            if (obm.matches()) {
                int count = Integer.parseInt(obm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, obm.group()).withMessage("message.neoblock.trader_spawned", "GLOBAL");
                onBlockActions.put(count, actions);
                NeoBlock.LOGGER.debug("Added on-block action {} for world.", key);
            }
            Matcher ebm = PatternUtil.EVERY_BLOCK_PATTERN.matcher(key);
            if (ebm.matches()) {
                int count = Integer.parseInt(ebm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, ebm.group()).withMessage("message.neoblock.trader_spawned", "GLOBAL");
                everyBlockActions.put(count, actions);
                NeoBlock.LOGGER.debug("Added on-every-block action {} for world.", key);
            }
        }
    }
    @Override public CompoundTag onSave(CompoundTag tag) {
        final CompoundTag mobs = new CompoundTag();
        tradedMobs.forEach((key, value) -> mobs.putInt(String.valueOf(MinecraftUtil.getEntityTypeResource(key)), value));
        tag.put("TradedMobs", mobs);

        final ListTag blocks = new ListTag();
        queue.forEach(block -> blocks.add(StringTag.valueOf(block.getID())));
        tag.put("Queue", blocks);

        final ListTag cools = new ListTag();
        cooldowns.forEach(cool -> cools.add(cool.save()));
        tag.put("Cooldowns", cools);

        return tag;
    }

    public WorldData(WorldManager data) {
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
        if (this.dimension == null || this.dimension.isEmpty()) return server.getLevel(Level.OVERWORLD);
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
    public boolean isOnCooldown() {
        return state == State.STOPPED;
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
    public void setOnCooldown() {
        state = State.STOPPED;
        data.setDirty();
    }

    public void addCooldown(WorldCooldown cooldown) {
        cooldowns.add(cooldown);
        setOnCooldown();

        BlockManager.BEDROCK_SPEC.placeAt(WorldManager.getWorldLevel(), NeoBlockPos.get());
    }
    public void removeCooldown(WorldCooldown cooldown) {
        cooldowns.remove(cooldown);
        if (cooldowns.isEmpty()) {
            setActive();
            BlockManager.updateBlock(WorldManager.getWorldLevel(), false);
        } else data.setDirty();
    }
    public @Nullable WorldCooldown getCooldown() {
        if (cooldowns.isEmpty()) return null;
        return cooldowns.get(0);
    }
    public int getBlockCount() {
        return blockCount;
    }
    public void setBlockCount(int count) {
        blockCount = count;
        for (int i: everyBlockActions.keySet()) if (count % i == 0) everyBlockActions.get(i).apply(WorldManager.getWorldLevel());
        if (onBlockActions.containsKey(count)) onBlockActions.get(count).apply(WorldManager.getWorldLevel());
        data.setDirty();
    }
    public void addBlockCount(int count) {
        setBlockCount(blockCount + count);
    }

    public int getLastTierSpawn() {
        return lastTierSpawn;
    }
    public void setLastTierSpawn(int tier) {
        this.lastTierSpawn = tier;
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
        UPDATED(3),     // NeoBlock configs has been updated / Incompatible HASH
        STOPPED(4);     // NeoBlock is running but is on cooldown

        private final int id;

        State(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static State fromId(int id) {
            for (State state : values()) if (state.id == id) return state;
            return INACTIVE;
        }
    }
}
