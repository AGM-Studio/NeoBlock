package xyz.agmstudio.neoblock.tiers;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;
import xyz.agmstudio.neoblock.tiers.merchants.NeoOffer;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Path;
import java.util.*;

public class NeoTier {
    public static final Path FOLDER = MinecraftUtil.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");

    public final CommentedFileConfig config;
    public final int id;
    public final int weight;

    public final String name;
    public final Lock lock;

    public final HashMap<BlockState, Integer> blocks = new HashMap<>();
    public final NeoMerchant tradeOffer;
    public final List<NeoOffer> trades;
    public final int tradeCount;

    protected NeoTier(int id) {
        this.id = id;
        config = ResourceUtil.getConfig(FOLDER, "tier-" + id);
        if (config == null) throw new RuntimeException("Unable to find config for tier " + id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", id);
        name = config.getOrElse("name", "Tier-" + id);

        UnmodifiableConfig lockConfig = config.get("unlock");
        lock = id > 0 ? lockConfig != null ? new Lock(this.id, lockConfig) : Lock.CommandOnly(this.id) : null;

        List<String> blocks = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks.stream().map(StringUtil::parseBlock).forEach(parsed -> this.blocks.merge(parsed.getKey().defaultBlockState(), parsed.getValue().get(), Integer::sum));
        if (this.blocks.isEmpty()) weight = 0;
        else weight = Math.max(0, config.getIntOrElse("weight", 1));

        List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        tradeOffer = NeoMerchant.parse(unlockTrades);

        List<String> trades = config.getOrElse("trader-trades", List.of());
        this.trades = trades.stream().map(NeoOffer::parse).filter(Objects::nonNull).toList();
        tradeCount = MinecraftUtil.MathUtil.clamp(config.getIntOrElse("trader-count", 0), 0, this.trades.size());
    }

    public List<NeoOffer> getRandomTrades() {
        List<NeoOffer> trades = new ArrayList<>(this.trades);
        Collections.shuffle(trades);
        return trades.subList(0, tradeCount);
    }
    public BlockState getRandomBlock() {
        if (blocks.isEmpty()) return NeoBlock.DEFAULT_STATE;

        int totalWeight = blocks.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = WorldData.getRandom().nextInt(totalWeight);
        for (Map.Entry<BlockState, Integer> entry: blocks.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue < 0) return entry.getKey();
        }

        NeoBlockMod.LOGGER.error("Unable to get a random block from tier {}", id);
        return blocks.keySet().stream().findFirst().orElse(NeoBlock.DEFAULT_STATE);
    }

    public String getName() {
        return name;
    }
    public Lock getLock() {
        return lock;
    }
    public int getWeight() {
        return weight;
    }

    public void onFinishUpgrade(ServerLevel level) {
        MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocked_tier", level, false, id);
    }
    public void onStartUpgrade(ServerLevel level) {
        MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocking_tier", level, false, id);
        if (tradeOffer != null) {
            tradeOffer.spawnTrader(level, "UnlockTrader");
            MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.unlocking_trader", level, false, id);
        }
    }

    public boolean isUnlocked() {
        return lock == null || lock.isUnlocked();
    }

    // Coding methods to help validate world using matching config.
    // Only hash game breaking data, no need for general data like blocks, weight, etc...
    protected String getHashCode() {
        String data = id + ":" + (lock == null ? "" : lock.hash());
        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(data.getBytes());
    }

    public boolean canBeUnlocked() {
        return !WorldData.getUnlocked().contains(this) && isUnlocked();
    }

    public static class Lock {
        private final int id;
        private final int time;         // Time to unlock.
        private final int blocks;       // Blocks broken to unlock.
        private final int game;         // Game time to unlock.
        private final boolean command;  // If command execution is needed to unlock.

        private Lock(int id, UnmodifiableConfig config) {
            this.id = id;
            time = config.getIntOrElse("unlock-time", 0);
            blocks = config.getIntOrElse("blocks", -1);
            game = config.getIntOrElse("game-time", -1);
            command = config.getOrElse("command", blocks < 0 && game < 0);
        }
        private Lock(int id, int time, int blocks, int game, boolean command) {
            this.id = id;
            this.time = time;
            this.blocks = blocks;
            this.game = game;
            this.command = command || (blocks < 0 && game < 0);
        }

        public static Lock CommandOnly(int id) {
            return new Lock(id, 0, -1, -1, true);
        }

        protected String hash() {
            return time + ":" + blocks + ":" + game + ":" + command;
        }

        public int getTime() {
            return time;
        }

        public boolean isUnlocked() {
            if (blocks > 0 && WorldData.getBlockCount() < blocks) return false;
            if (game > 0 && WorldData.getGameTime() < game) return false;
            return !command || WorldData.isCommanded(id);
        }
    }
}
