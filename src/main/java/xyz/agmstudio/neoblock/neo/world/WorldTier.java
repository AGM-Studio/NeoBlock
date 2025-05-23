package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.data.TierLock;
import xyz.agmstudio.neoblock.neo.merchants.NeoOffer;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WorldTier extends NBTSaveable {
    public static WorldTier of(@NotNull TierData tier, WorldData data) {
        WorldTier instance = new WorldTier();
        instance.id = tier.id;
        instance.lock = new Lock();
        instance.lock.unlocked = tier.lock.isUnlocked(data);
        instance.lock.tier = instance;
        instance.enabled = instance.lock.unlocked;

        instance.data = tier;
        instance.world = data;

        return instance;
    }

    /** The id of this Tier. Connects to data loaded from the config. */
    @NBTData private int id;
    /** If the tier should be counted in randomizer. */
    @NBTData private boolean enabled;
    /** Upgrade status if needed. */
    @NBTData protected Lock lock;

    private TierData data = null;
    private WorldData world = null;
    @Override public void onLoad(CompoundTag tag) {
        data = TierData.get(id);
        world = WorldData.getInstance();

        if (lock == null) lock = new Lock();
        lock.tier = this;
    }

    public List<NeoOffer> getRandomTrades() {
        List<NeoOffer> trades = new ArrayList<>(data.trades);
        Collections.shuffle(trades);
        return trades.subList(0, data.tradeCount);
    }
    public BlockState getRandomBlock() {
        if (data.blocks.isEmpty()) return WorldData.DEFAULT_STATE;

        int totalWeight = data.blocks.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = WorldData.getRandom().nextInt(totalWeight);
        for (Map.Entry<BlockState, Integer> entry: data.blocks.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue < 0) return entry.getKey();
        }

        NeoBlockMod.LOGGER.error("Unable to get a random block from tier {}", id);
        return data.blocks.keySet().stream().findFirst().orElse(WorldData.DEFAULT_STATE);
    }

    public @NotNull String getName() {
        return data.name;
    }
    public @NotNull TierLock getLock() {
        return data.lock;
    }
    public int getWeight() {
        return data.weight;
    }

    public void onFinishUpgrade(ServerLevel level) {
        this.enable();
        MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocked_tier", level, false, id);
    }
    public void onStartUpgrade(ServerLevel level) {
        MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocking_tier", level, false, id);
        if (data.tradeOffer != null) {
            data.tradeOffer.spawnTrader(level, "UnlockTrader");
            MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocking_trader", level, false, id);
        }
    }

    public boolean isUnlocked() {
        return lock.unlocked;
    }
    public boolean canBeUnlocked() {
        return !isUnlocked() && data.lock.isUnlocked(WorldData.getInstance());
    }

    public boolean isCommanded() {
        return lock.commanded;
    }

    public boolean isEnabled() {
        return lock.unlocked && enabled;
    }
    public void enable() {
        enabled = true;
    }
    public void disable() {
        enabled = false;
    }

    public int getID() {
        return id;
    }
    public TierData getData() {
        return data;
    }

    public static class Lock extends NBTSaveable {
        public WorldTier tier;

        @NBTData protected boolean unlocked = false;
        // Conditions - Should match with tier data - For future updates
        @NBTData protected boolean commanded = false;
        // The lock process
        @NBTData protected int line = 0;
        @NBTData protected int tick = 0;

        public boolean isDone() {
            return tick >= tier.data.lock.getTime();
        }
        public int getGoal() {
            return tier.data.lock.getTime();
        }
    }
}
