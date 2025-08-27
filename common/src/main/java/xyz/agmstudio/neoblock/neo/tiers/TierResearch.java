package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.util.MessengerUtil;

public class TierResearch implements NBTSaveable {
    protected final TierSpec tier;

    @NBTData protected boolean done = false;
    @NBTData protected long tick = 0;

    protected long time = 72000;

    @Override public CompoundTag onSave(CompoundTag tag) {
        tag.putInt("tier", tier.id);
        return tag;
    }

    public TierResearch(TierSpec root) {
        this.tier = root;
    }

    public void onFinish(ServerLevel level) {
        tier.enable();
        tier.startSequence.addToQueue(false);
        MessengerUtil.sendInstantMessage("message.neoblock.unlocked_tier", level, false, tier.id);
    }
    public void onStart(ServerLevel level) {
        MessengerUtil.sendInstantMessage("message.neoblock.unlocking_tier", level, false, tier.id);
        tier.unlockActions.apply(level);
    }

    public long getTime() {
        return time;
    }
    public boolean isTimeDone() {
        return tick >= time;
    }

    @Override public String toString() {
        return "TierResearch[tier=%d, tick=%d, time=%d, done=%s]".formatted(tier.id, tick, time, done);
    }
}
