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
import xyz.agmstudio.neoblock.neo.NeoBlock;
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
        instance.enabled = tier.lock.isUnlocked(data);
        instance.upgraded = instance.enabled;
        instance.unlocked = instance.enabled;
        instance.process = false;

        instance.data = tier;
        instance.world = data;

        return instance;
    }

    /** The id of this Tier. Connects to data loaded from the config. */
    @NBTData private int id;
    /** If the tier should be counted in randomizer. */
    @NBTData private boolean enabled;
    /** If the tier has been researched. */
    @NBTData private boolean upgraded;
    /** If the tier has been unlocked using commands. */
    @NBTData private boolean unlocked;
    /** Upgrade status if needed. */
    @NBTData private boolean process;

    private TierData data = null;
    private WorldData world = null;
    @Override public void onLoad(CompoundTag tag) {
        data = TierData.get(id);
        world = WorldData.getInstance();
    }

    public List<NeoOffer> getRandomTrades() {
        List<NeoOffer> trades = new ArrayList<>(data.trades);
        Collections.shuffle(trades);
        return trades.subList(0, data.tradeCount);
    }
    public BlockState getRandomBlock() {
        if (data.blocks.isEmpty()) return NeoBlock.DEFAULT_STATE;

        int totalWeight = data.blocks.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = WorldData.getRandom().nextInt(totalWeight);
        for (Map.Entry<BlockState, Integer> entry: data.blocks.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue < 0) return entry.getKey();
        }

        NeoBlockMod.LOGGER.error("Unable to get a random block from tier {}", id);
        return data.blocks.keySet().stream().findFirst().orElse(NeoBlock.DEFAULT_STATE);
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
        return unlocked;
    }
    public boolean canBeUnlocked() {
        return data.lock.isUnlocked(WorldData.getInstance());
    }

    public boolean isEnabled() {
        return upgraded && enabled;
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
}
