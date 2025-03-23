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
    private static final String DATA_NAME = "neo_block_data";
    private static WorldData instance;

    public static WorldData getInstance() {
        return instance;
    }

    public static @NotNull WorldData load(@NotNull ServerLevel level) {
        WorldData data = level.getDataStorage().computeIfAbsent(new Factory<>(WorldData::create, WorldData::load), DATA_NAME);
        data.level = level;
        return data;
    }
    private static @NotNull WorldData create() {
        WorldData data = new WorldData();

        for (NeoTier tier: NeoBlock.TIERS) {
            data.encoding.add(tier.getHashCode());
            if (tier.TIER == 0 || tier.isUnlocked())
                data.unlocked.add(tier);
        }

        NeoBlockMod.LOGGER.debug("Creating new world data");
        return data;
    }
    private static @NotNull WorldData load(@NotNull CompoundTag tag, HolderLookup.Provider lookupProvider) {
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

        ListTag unlocked = tag.getList("Unlocked", StringTag.TAG_INT);
        for (int i = 0; i < unlocked.size(); ++i) data.unlockedIDs.add(unlocked.getInt(i));

        NeoBlockMod.LOGGER.debug("Loaded WorldData from {}", tag);

        if (WorldData.isValid()) {
            for (int i: data.unlockedIDs) data.unlocked.add(NeoBlock.TIERS.get(i));
        } else {
            data.state = WorldState.UPDATED;
            NeoBlockMod.LOGGER.warn("Tiers has been modified. NeoBlock will be disabled till (/neoblock force update) is executed.");
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

    private ServerLevel level = null;
    private WorldState state = WorldState.INACTIVE;
    private int blockCount = 0;
    private int traderFailedAttempts = 0;

    private final HashSet<Integer> unlockedIDs = new HashSet<>();
    private final HashSet<NeoTier> unlocked = new HashSet<>();
    private final HashSet<String> encoding = new HashSet<>();

    private final UpgradeManager upgrade = new UpgradeManager();
    private final HashMap<EntityType<?>, Integer> tradedMobs = new HashMap<>();

    private WorldData() {
        instance = this;
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
    public static UpgradeManager getUpgradeManager() {
        return instance.upgrade;
    }

    public static boolean isValid() {
        return NeoBlock.hash.equals(instance.encoding);
    }
    public static boolean updateTiers() {
        instance.encoding.clear();
        instance.unlocked.clear();
        instance.unlockedIDs.clear();
        for (NeoTier tier: NeoBlock.TIERS) {
            instance.encoding.add(tier.getHashCode());
            if (tier.TIER == 0 || tier.isUnlocked()) {
                instance.unlocked.add(tier);
                instance.unlockedIDs.add(tier.TIER);
            }
        }

        instance.encoding.clear();
        NeoBlock.TIERS.stream().map(NeoTier::getHashCode).forEach(instance.encoding::add);

        return true;
    }
}
