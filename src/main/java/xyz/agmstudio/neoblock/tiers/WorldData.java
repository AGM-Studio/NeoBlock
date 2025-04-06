package xyz.agmstudio.neoblock.tiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.HashMap;
import java.util.HashSet;

public class WorldData extends MinecraftUtil.AbstractWorldData {
    private static WorldData instance;
    public static WorldData getInstance() {
        return instance;
    }

    public static @NotNull WorldData create(@NotNull ServerLevel level) {
        WorldData data = new WorldData(level);

        for (NeoTier tier: NeoBlock.TIERS) {
            data.encoding.add(tier.getHashCode());
            if (tier.id == 0 || tier.isUnlocked())
                data.unlocked.add(tier);
        }

        NeoBlockMod.LOGGER.debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldData load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldData data = new WorldData(level);
        data.state = WorldState.fromId(tag.getInt("WorldState"));
        data.blockCount = tag.getInt("BlockCount");
        data.traderFailedAttempts = tag.getInt("TraderFailedAttempts");

        final ListTag upgrade = tag.getList("Upgrades", StringTag.TAG_COMPOUND);
        data.tierManager.load(upgrade);

        final CompoundTag mobs = tag.getCompound("TradedMobs");
        mobs.getAllKeys().forEach(key -> data.tradedMobs.merge(MinecraftUtil.getEntityType(key), mobs.getInt(key), Integer::sum));

        final ListTag hash = tag.getList("Encoding", StringTag.TAG_STRING);
        for (int i = 0; i < hash.size(); ++i) data.encoding.add(hash.getString(i));

        final ListTag unlocked = tag.getList("Unlocked", StringTag.TAG_INT);
        for (int i = 0; i < unlocked.size(); ++i) data.unlockedIDs.add(unlocked.getInt(i));

        final ListTag commanded = tag.getList("Commanded", StringTag.TAG_INT);
        for (int i = 0; i < commanded.size(); ++i) data.commanded.add(commanded.getInt(i));

        NeoBlockMod.LOGGER.debug("Loaded WorldData from {}", tag);

        if (WorldData.isValid()) for (int i : data.unlockedIDs) data.unlocked.add(NeoBlock.TIERS.get(i));
        else data.state = WorldState.UPDATED;

        return data;
    }

    @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.putInt("WorldState", state.getId());
        tag.putInt("BlockCount", blockCount);
        tag.putInt("TraderFailedAttempts", traderFailedAttempts);

        this.tierManager.save(tag);

        final CompoundTag mobs = new CompoundTag();
        tradedMobs.forEach((key, value) -> mobs.putInt(String.valueOf(MinecraftUtil.getEntityTypeResource(key)), value));
        tag.put("TradedMobs", mobs);

        final ListTag hash = new ListTag();
        encoding.stream().map(StringTag::valueOf).forEach(hash::add);
        tag.put("Encoding", hash);

        final ListTag utg = new ListTag();
        unlocked.forEach(tier -> utg.add(IntTag.valueOf(tier.id)));
        tag.put("Unlocked", utg);

        final ListTag ctg = new ListTag();
        commanded.forEach(id -> ctg.add(IntTag.valueOf(id)));
        tag.put("Commanded", ctg);

        NeoBlockMod.LOGGER.debug("Saving WorldData to {}", tag);
        return tag;
    }

    private final ServerLevel level;
    private WorldState state = WorldState.INACTIVE;
    private int blockCount = 0;
    private int traderFailedAttempts = 0;

    private final HashSet<Integer> unlockedIDs = new HashSet<>();
    private final HashSet<NeoTier> unlocked = new HashSet<>();
    private final HashSet<Integer> commanded = new HashSet<>();
    private final HashSet<String> encoding = new HashSet<>();

    private final TierManager tierManager = new TierManager();
    private final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();

    private WorldData(ServerLevel level) {
        instance = this;
        this.level = level;
    }

    public static boolean isInactive() {
        return instance.state == WorldState.INACTIVE;
    }
    public static void setActive() {
        instance.state = WorldState.ACTIVE;
        instance.setDirty();
    }
    public static boolean isActive() {
        return instance.state == WorldState.ACTIVE;
    }
    public void setDisabled() {
        instance.state = WorldState.DISABLED;
        instance.setDirty();
    }
    public static boolean isDisabled() {
        return instance.state == WorldState.DISABLED;
    }
    public static void setUpdated() {
        instance.state = WorldState.UPDATED;
        instance.setDirty();
    }
    public static boolean isUpdated() {
        return instance.state == WorldState.UPDATED;
    }

    public static int getBlockCount() {
        return instance.blockCount;
    }
    public static void setBlockCount(int count) {
        instance.blockCount = count;
        instance.setDirty();
    }
    public static void addBlockCount(int count) {
        instance.blockCount += count;
        instance.setDirty();
    }

    public static long getGameTime() {
        return instance.level.getGameTime();
    }

    public static int getTraderFailedAttempts() {
        return instance.traderFailedAttempts;
    }
    public static void resetTraderFailedAttempts() {
        instance.traderFailedAttempts = 0;
        instance.setDirty();
    }
    public static void addTraderFailedAttempts() {
        instance.traderFailedAttempts += 1;
        instance.setDirty();
    }

    public static HashMap<EntityType<?>, Integer> getTradedMobs() {
        return instance.tradedMobs;
    }
    public static void addTradedMob(EntityType<?> entityType, int count) {
        instance.tradedMobs.merge(entityType, count, Integer::sum);
        instance.setDirty();
    }
    public static void clearTradedMobs() {
        instance.tradedMobs.clear();
        instance.setDirty();
    }

    public static HashSet<NeoTier> getUnlocked() {
        return instance.unlocked;
    }
    public static void unlockTier(NeoTier tier) {
        instance.unlocked.add(tier);
    }
    public static TierManager getTierManager() {
        return instance.tierManager;
    }

    public static boolean isCommanded(int id) {
        return instance.commanded.contains(id);
    }
    public static void setCommanded(int id) {
        instance.setDirty();
        instance.commanded.add(id);
    }

    public static boolean isValid() {
        return NeoBlock.hash.equals(instance.encoding);
    }
    public static void updateTiers() {
        instance.encoding.clear();
        instance.unlocked.clear();
        for (NeoTier tier: NeoBlock.TIERS) {
            instance.encoding.add(tier.getHashCode());
            if (tier.id == 0 || tier.isUnlocked() || instance.unlockedIDs.contains(tier.id))
                instance.unlocked.add(tier);
        }
        instance.unlockedIDs.clear();
        instance.unlocked.forEach(tier -> instance.unlockedIDs.add(tier.id));
    }

    public static @NotNull RandomSource getRandom() {
        return instance.level.getRandom();
    }

    public static void tick(ServerLevel level, LevelAccessor access) {
        if (instance != null) instance.tierManager.tick(level, access);
    }
}
