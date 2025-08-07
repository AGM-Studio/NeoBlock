package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.compatibility.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;

public class TierResearch implements NBTSaveable {
    protected final TierSpec tier;

    @NBTData protected boolean done = false;
    @NBTData protected long tick = 0;

    protected long time = 72000;

    public TierResearch(TierSpec root) {
        this.tier = root;
    }

    public void onFinish(ServerLevel level) {
        tier.enable();
        tier.startSequence.addToQueue(false);
        MessengerAPI.sendInstantMessage("message.neoblock.unlocked_tier", level, false, tier.id);
    }
    public void onStart(ServerLevel level) {
        MessengerAPI.sendInstantMessage("message.neoblock.unlocking_tier", level, false, tier.id);
        WanderingTrader trader = NeoMerchant.spawnTraderWith(tier.tradePoolUnlock.getPool(), level, "UnlockTrader");
        if (trader != null) MessengerAPI.sendInstantMessage("message.neoblock.unlocking_trader", level, false, tier.id);
    }

    public long getTime() {
        return time;
    }
    public boolean isTimeDone() {
        return tick >= time;
    }
}
