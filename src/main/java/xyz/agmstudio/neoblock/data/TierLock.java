package xyz.agmstudio.neoblock.data;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

public class TierLock {
    private final int id;
    private final int time;         // Time to unlock.
    private final int blocks;       // Blocks broken to unlock.
    private final int game;         // Game time to unlock.
    private final boolean command;  // If command execution is needed to unlock.

    TierLock(int id, UnmodifiableConfig config) {
        this.id = id;
        time = config.getIntOrElse("unlock-time", 0);
        blocks = config.getIntOrElse("blocks", -1);
        game = config.getIntOrElse("game-time", -1);
        command = config.getOrElse("command", blocks < 0 && game < 0);
    }

    private TierLock(int id, int time, int blocks, int game, boolean command) {
        this.id = id;
        this.time = time;
        this.blocks = blocks;
        this.game = game;
        this.command = command || (blocks < 0 && game < 0);
    }

    public static TierLock CommandOnly(int id) {
        return new TierLock(id, 0, -1, -1, true);
    }
    public static TierLock Unlocked() {
        return new TierLock(0, 0, -1, -1, true) {
            @Override public boolean isUnlocked(WorldData data) {
                return true;
            }
            @Override protected String hash() {
                return "";
            }
        };
    }

    protected String hash() {
        return "V0.5" + time + ":" + blocks + ":" + game + ":" + command;
    }

    public int getTime() {
        return time;
    }
    public int getBlocks() {
        return blocks;
    }
    public int getGameplay() {
        return game;
    }
    public boolean isCommanded() {
        return command;
    }

    public boolean isUnlocked(WorldData data) {
        return isUnlocked(data, data.getTier(id));
    }
    public boolean isUnlocked(WorldData data, WorldTier tier) {
        assert tier == null || tier.getID() == id;
        if (id == 0) return true;
        if (blocks > 0 && data.getStatus().getBlockCount() < blocks) return false;
        if (game > 0 && data.getLevel().getGameTime() < game) return false;
        return !command || tier == null || tier.isCommanded();
    }
}
