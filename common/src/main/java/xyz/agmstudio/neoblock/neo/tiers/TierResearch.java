package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.data.NBTSaveable;

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
        tier.unlockActions.applyRulesAndCommands(level);
        NeoBlock.sendInstantMessage("message.neoblock.unlocked_tier", level, false, tier.id);
    }
    public void onStart(ServerLevel level) {
        NeoBlock.sendInstantMessage("message.neoblock.unlocking_tier", level, false, tier.id);
        tier.unlockActions.applyTrader(level);
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

    public long advanceBy(long value) {
        this.tick = Math.max(1, Math.min(this.tick + value, time));
        return this.tick;
    }
}
