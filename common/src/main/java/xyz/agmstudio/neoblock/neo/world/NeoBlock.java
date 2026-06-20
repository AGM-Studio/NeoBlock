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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.NeoListener;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.events.NeoEventAction;
import xyz.agmstudio.neoblock.neo.events.NeoEventBlockTrigger;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.data.NBTSaveable;
import xyz.agmstudio.neocore.platform.IConfig;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

public class NeoBlock implements NBTSaveable {
    public static final NeoBlockSpec DEFAULT_SPEC = new NeoBlockSpec(Blocks.GRASS_BLOCK);
    public static final NeoBlockSpec BEDROCK_SPEC = new NeoBlockSpec(Blocks.BEDROCK);
    public static final double AABB_RANGE = 1.0;

    public final WorldManager world;
    public final ServerLevel level;

    @NBTData("State") protected State state = State.INACTIVE;
    @NBTData("BlockCount") protected int blockCount = 0;
    @NBTData("LastTierSpawn") protected int lastTierSpawn = 0;
    @NBTData("TraderFailedAttempts") protected int traderFailedAttempts = 0;
    @NBTData("Position") protected BlockPos pos = new BlockPos(0, 64, 0);
    @NBTData("Dimension") protected String dimension = "minecraft:overworld";

    protected final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();
    protected final List<NeoBlockSpec> queue = new ArrayList<>();
    protected final List<WorldCooldown> cooldowns = new ArrayList<>();

    protected final LinkedHashMap<Integer, NeoEventAction> onBlockActions = new LinkedHashMap<>();
    protected final LinkedHashMap<Integer, NeoEventAction> everyBlockActions = new LinkedHashMap<>();

    public NeoBlock(@NotNull ServerLevel level, @NotNull WorldManager world) {
        this.level = level;
        this.world = world;
    }

    @Override public void onLoad(@NotNull CompoundTag tag) {
        final CompoundTag mobs = tag.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> tradedMobs.merge(NeoMC.getEntityType(key).orElse(null), mobs.getInt(key), Integer::sum));

        queue.clear();
        final ListTag blocks = tag.getList("Queue", Tag.TAG_STRING);
        blocks.forEach(block -> NeoBlockSpec.parse(block.getAsString()).ifPresent(queue::add));

        cooldowns.clear();
        final ListTag cools = tag.getList("Cooldowns", Tag.TAG_COMPOUND);
        cools.forEach(cool -> {
            WorldCooldown cooldown = NBTSaveable.instance(WorldCooldown.class, (CompoundTag) cool);
            cooldowns.add(cooldown);
        });

        IConfig config = NeoBlockMod.getConfig();
        for (String key: config.keys()) {
            Matcher obm = NeoEventBlockTrigger.ON_BLOCK_PATTERN.matcher(key);
            if (obm.matches()) {
                int count = Integer.parseInt(obm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, obm.group()).withMessage("message.neoblock.trader_spawned", "GLOBAL");
                onBlockActions.put(count, actions);
                NeoBlockMod.getLogger().debug("Added on-block action {} for world.", key);
            }
            Matcher ebm = NeoEventBlockTrigger.EVERY_BLOCK_PATTERN.matcher(key);
            if (ebm.matches()) {
                int count = Integer.parseInt(ebm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, ebm.group()).withMessage("message.neoblock.trader_spawned", "GLOBAL");
                everyBlockActions.put(count, actions);
                NeoBlockMod.getLogger().debug("Added on-every-block action {} for world.", key);
            }
        }
    }
    @Override public CompoundTag onSave(@NotNull CompoundTag tag) {
        final CompoundTag mobs = new CompoundTag();
        tradedMobs.forEach((key, value) -> mobs.putInt(String.valueOf(NeoMC.getEntityTypeResource(key)), value));
        tag.put("TradedMobs", mobs);

        final ListTag blocks = new ListTag();
        queue.forEach(block -> blocks.add(StringTag.valueOf(block.getID())));
        tag.put("Queue", blocks);

        final ListTag cools = new ListTag();
        cooldowns.forEach(cool -> cools.add(cool.save()));
        tag.put("Cooldowns", cools);

        return tag;
    }

    public boolean isCorrectDimension(@NotNull ServerLevel level) {
        return isCorrectDimension(level.dimension());
    }
    public boolean isCorrectDimension(@NotNull ResourceKey<Level> dimension) {
        return dimension.location().toString().equals(this.dimension);
    }
    public ServerLevel getDimension() {
        return getDimension(world.getLevel().getServer());
    }
    public ServerLevel getDimension(MinecraftServer server) {
        if (this.dimension == null || this.dimension.isEmpty()) return server.getLevel(Level.OVERWORLD);
        @NotNull ResourceLocation location = NeoMC.parseResourceLocation(this.dimension);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
        return server.getLevel(dimension);
    }
    public BlockPos getBlockPos() {
        return pos;
    }
    public BlockPos safeBlock() {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
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
        world.setDirty();
    }
    public void setActive() {
        state = State.ACTIVE;
        world.setDirty();
    }
    public void setDisabled() {
        state = State.DISABLED;
        world.setDirty();
    }
    public void setUpdated() {
        state = State.UPDATED;
        world.setDirty();
    }
    public void setOnCooldown() {
        state = State.STOPPED;
        world.setDirty();
    }

    public void addCooldown(WorldCooldown cooldown) {
        cooldowns.add(cooldown);
        setOnCooldown();

        BEDROCK_SPEC.placeAt(this);
    }
    public void removeCooldown(WorldCooldown cooldown) {
        cooldowns.remove(cooldown);
        if (cooldowns.isEmpty()) {
            setActive();
            updateBlock(WorldManager.getWorldLevel(), false);
        } else world.setDirty();
    }
    public List<WorldCooldown> getCooldowns() {
        return cooldowns;
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
        world.setDirty();
    }
    public void addBlockCount(int count) {
        setBlockCount(blockCount + count);
    }

    public int getLastTierSpawn() {
        return lastTierSpawn;
    }
    public void setLastTierSpawn(int tier) {
        this.lastTierSpawn = tier;
        world.setDirty();
    }

    public int getTraderFailedAttempts() {
        return traderFailedAttempts;
    }
    public void resetTraderFailedAttempts() {
        traderFailedAttempts = 0;
        world.setDirty();
    }
    public int addTraderFailedAttempts() {
        traderFailedAttempts += 1;
        world.setDirty();

        return traderFailedAttempts;
    }

    public HashMap<EntityType<?>, Integer> getTradedMobs() {
        return tradedMobs;
    }
    public void addTradedMob(EntityType<?> entityType, int count) {
        tradedMobs.merge(entityType, count, Integer::sum);
        world.setDirty();
    }
    public void clearTradedMobs() {
        tradedMobs.clear();
        world.setDirty();
    }

    public void setDimension(@NotNull ServerLevel level) {
        setDimension(level.dimension());
    }
    public void setDimension(@NotNull ResourceKey<Level> dimension) {
        this.dimension = dimension.location().toString();
        world.setDirty();
    }
    public void setBlockPos(BlockPos pos, ServerLevel level) {
        cleanBlock(level, this.pos);

        this.pos = pos;
        world.setDirty();

        level.setDefaultSpawnPos(this.pos, 0.0f);
    }
    public void cleanBlock(ServerLevel level, BlockPos pos) {
        BlockState block = getCurrentBlock(level);
        if (block.getBlock().equals(Blocks.BEDROCK))
            DEFAULT_SPEC.placeAt(this);
    }

    public void initiate(@NotNull ServerLevel level) {
        TierSpec tier0 = WorldManager.getWorldTier(0);
        if (tier0 != null) tier0.getStartSequence().addToQueue(this, false);
        updateBlock(level, false);
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

    public void ensureNoFall() {
        Vec3 center = pos.getCenter();
        for(Entity entity: level.getEntities(null, AABB.ofSize(center, AABB_RANGE, AABB_RANGE, AABB_RANGE)))
            entity.teleportTo(entity.getX(), center.y + AABB_RANGE / 2.0, entity.getZ());
    }

    public TierSpec getRandomTierSpec() {
        AtomicInteger totalChance = new AtomicInteger();
        List<TierSpec> tiers = new ArrayList<>();

        WorldManager.getWorldTiers().stream().filter(TierSpec::isEnabled).forEach(tier -> {
            tiers.add(tier);
            totalChance.addAndGet(tier.getWeight());
        });

        if (totalChance.get() == 0) return null;
        int randomValue = WorldManager.getRandom().nextInt(totalChance.get());
        for (TierSpec tier : tiers) {
            randomValue -= tier.getWeight();
            if (randomValue < 0) return tier;
        }

        return null;
    }
    public NeoBlockSpec getRandomBlock() {
        Optional<NeoBlockSpec> queued = getNextInQueue();
        if (queued.isPresent()) return queued.get();

        TierSpec tier = getRandomTierSpec();
        if (tier == null) {
            NeoBlockMod.getLogger().error("Unable to find a block for {} blocks", getBlockCount());
            return DEFAULT_SPEC;
        }

        setLastTierSpawn(tier.getID());
        return tier.getRandomBlock();
    }

    public void updateBlock(ServerLevel level, boolean trigger) {
        int lastSpawnTier = getLastTierSpawn();
        if (state == State.ACTIVE) getRandomBlock().placeAt(this);
        else BEDROCK_SPEC.placeAt(this);  // Creative cheaters & Move block in mid-search (Just in case)

        if (!trigger) return;
        addBlockCount(1);
        Animation.resetIdleTick();
        NeoListener.execute(() -> NeoMerchant.attemptSpawnTrader(level));
        TierSpec last = WorldManager.getWorldTier(lastSpawnTier);
        if (last != null) last.addCount(1);

        for (TierSpec tier: WorldManager.getWorldTiers())
            if (tier.canBeResearched()) tier.startResearch();
    }

    public BlockState getCurrentBlock(ServerLevel level) {
        return level.getBlockState(pos);
    }

    public void tick() {
        final BlockState block = level.getBlockState(pos);
        if (state == State.UPDATED || state == State.STOPPED) {
            if (block.getBlock() != Blocks.BEDROCK) BEDROCK_SPEC.placeAt(this);
        } else if (block.isAir() || block.canBeReplaced()) updateBlock(level, true);
    }

    public boolean isNeoBlock(ServerLevel level, BlockPos pos) {
        return isCorrectDimension(level) && getBlockPos().equals(pos);
    }

    public static void handleEndPortalFrameBreak(ServerLevel level, BlockState state, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        if (!NeoMC.canBreak(tool.getItem(), Blocks.OBSIDIAN.defaultBlockState())) return;

        ItemStack drop = NeoMC.isSilkTouched(tool) ?
                new ItemStack(Blocks.END_PORTAL_FRAME) :
                new ItemStack(Blocks.END_STONE);

        Block.popResource(level, pos, drop);
    }
}