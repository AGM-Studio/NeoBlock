package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NBTData;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Path;
import java.util.*;

public class TierSpec implements NBTSaveable {
    public static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");

    // Stored data in world info
    @NBTData protected int id;
    @NBTData protected boolean enabled = false;
    @NBTData protected boolean commanded = false;
    @NBTData protected TierResearch research;
    @NBTData protected String hash = "";

    public TierSpec() {
        // Normal version for WorldData to load with
    }
    public TierSpec(final int id) {
        // Extended version for the WorldData to reset with
        this.id = id;
        this.research = new TierResearch(this);

        this.onLoad(new CompoundTag());
        this.hash = getHashCode();
    }

    // Data loaded from config
    protected String name;
    protected int weight;

    protected final HashSet<TierRequirement> requirements = new HashSet<>();

    protected final List<NeoBlockSpec> blocks = new ArrayList<>();
    protected int totalBlockWeight = 0;

    protected NeoTradePool tradePoolUnlock;
    protected NeoTradePool trades;
    protected NeoSeqBlockSpec startSequence;

    @Override public void onLoad(CompoundTag tag) {
        TierManager.loadConfig(this);

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
    public List<NeoBlockSpec> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }
    public double getTotalBlockWeight() {
        return totalBlockWeight;
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
    public void startResearch() {
        if (canBeResearched()) TierManager.addResearch(this.research);
    }
    public TierResearch getResearch() {
        if (research == null) research = new TierResearch(this);
        return research;
    }

    public void setSpecialRequirement(boolean special) {
        this.commanded = special;
    }
    public Set<TierRequirement> getRequirements() {
        return Collections.unmodifiableSet(requirements);
    }
    public boolean hasSpecialRequirement() {
        for (TierRequirement requirement: requirements)
            if (requirement instanceof TierRequirement.Special) return true;

        return false;
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
