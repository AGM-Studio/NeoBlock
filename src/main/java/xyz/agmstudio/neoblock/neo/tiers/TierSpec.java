package xyz.agmstudio.neoblock.neo.tiers;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class TierSpec implements NBTSaveable {
    public static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");

    // Stored data in world info
    @NBTData protected int id;
    @NBTData protected boolean enabled = false;
    @NBTData protected boolean commanded = false;
    @NBTData protected TierResearch research;
    @NBTData protected String hash = "";

    // Data loaded from config
    protected String name;
    protected int weight;

    protected final HashSet<TierRequirement> requirements = new HashSet<>();

    protected final List<NeoBlockSpec> blocks = new ArrayList<>();
    private int totalBlockWeight = 0;

    protected NeoTradePool tradePoolUnlock;
    protected NeoTradePool trades;

    @Override public void onLoad(CompoundTag tag) {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "tier-" + this.id);
        if (config == null) throw new AbortException("Unable to find config for tier " + this.id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", this.id);
        this.name = config.getOrElse("name", "Tier-" + this.id);

        final UnmodifiableConfig lockConfig = config.get("unlock");
        this.research.time = config.getIntOrElse("unlock-time", 0);
        if (this.id > 0) {
            long time = config.getIntOrElse("game-time", -1);
            if (time > 0) this.requirements.add(new TierRequirement.GameTime(time));
            long blocks = config.getIntOrElse("blocks", -1);
            if (blocks > 0) this.requirements.add(new TierRequirement.BlockBroken(blocks));
            if (config.getOrElse("command", this.requirements.isEmpty()))
                this.requirements.add(new TierRequirement.Special());
        }

        final List<String> blocks_list = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(this.blocks::add));

        if (this.blocks.isEmpty()) this.weight = 0;
        else this.weight = Math.max(0, config.getIntOrElse("weight", 1));

        final List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        this.tradePoolUnlock = NeoTradePool.parse(unlockTrades);

        final List<String> list = config.getOrElse("trader-trades", config.getOrElse("trades", List.of()));
        this.trades = NeoTradePool.parse(list);

        NeoBlockMod.LOGGER.debug("Tier {} loaded. Hash key: {}", this.id, this.getHashCode());

        totalBlockWeight = blocks.stream().mapToInt(NeoBlockSpec::getWeight).sum();
    }

    public boolean isStable() {
        return Objects.equals(hash, getHashCode());
    }

    public String getHashCode() {
        StringBuilder data = new StringBuilder(id + ":");
        for (TierRequirement requirement: requirements)
            data.append(requirement.hash()).append(":");

        return StringUtil.encodeToBase64(data.toString());
    }

    public List<NeoTrade> getTrades() {
        return trades.getPool();
    }
    public NeoBlockSpec getRandomBlock() {
        if (blocks.isEmpty()) return BlockManager.DEFAULT_SPEC;

        int randomValue = WorldData.getRandom().nextInt(totalBlockWeight);
        for (NeoBlockSpec entry: blocks) {
            randomValue -= entry.getWeight();
            if (randomValue < 0) return entry;
        }

        NeoBlockMod.LOGGER.error("Unable to get a random block from tier {}", id);
        return blocks.stream().findFirst().orElse(BlockManager.DEFAULT_SPEC);
    }

    public @NotNull String getName() {
        return name;
    }
    public int getWeight() {
        return weight;
    }

    public boolean isResearched() {
        return research.done;
    }
    public boolean canBeResearched() {
        if (research.done) return false;
        for (TierRequirement requirement: requirements)
            if (!requirement.isMet(WorldData.getInstance(), this)) return false;

        return true;
    }

    public boolean isEnabled() {
        return research.done && enabled;
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
}
