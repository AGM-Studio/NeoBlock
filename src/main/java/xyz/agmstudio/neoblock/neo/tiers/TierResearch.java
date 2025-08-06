package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;

public class TierResearch implements NBTSaveable {
    private final TierSpec root;

    @NBTData public boolean done = false;
    @NBTData public long tick = 0;

    public long time = 72000;

    public TierResearch(TierSpec root) {
        this.root = root;
    }

    public void onFinish(ServerLevel level) {
        root.enable();
        MessengerAPI.sendInstantMessage("message.neoblock.unlocked_tier", level, false, root.id);
    }
    public void onStartUpgrade(ServerLevel level) {
        MessengerAPI.sendInstantMessage("message.neoblock.unlocking_tier", level, false, root.id);
        NeoMerchant.spawnTraderWith(root.tradePoolUnlock.getPool(), level, "UnlockTrader");
        MessengerAPI.sendInstantMessage("message.neoblock.unlocking_trader", level, false, root.id);
    }
}
