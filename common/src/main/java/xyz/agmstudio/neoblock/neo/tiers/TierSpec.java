package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.events.NeoEventAction;
import xyz.agmstudio.neoblock.neo.events.NeoEventBlockTrigger;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Path;
import java.util.*;

public class TierSpec implements NBTSaveable {
    public static final Path FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID, "tiers");

    // Stored data in world info
    @NBTData protected int id;
    @NBTData protected int count = 0;
    @NBTData protected boolean enabled;
    @NBTData protected boolean commanded = false;
    @NBTData protected boolean researched = false;
    @NBTData protected String hash = "";

    public TierSpec(final int id, boolean loadConfig) {
        this.id = id;
        this.enabled = id == 0;

        if (loadConfig) {
            TierManager.loadTierConfig(this);
            this.totalBlockWeight = blocks.stream().mapToInt(NeoBlockSpec::getWeight).sum();
        }

        this.hash = getHashCode();
    }

    @Override
    public void onLoad(CompoundTag tag) {
        TierManager.loadTierConfig(this);

        this.totalBlockWeight = blocks.stream().mapToInt(NeoBlockSpec::getWeight).sum();
    }

    // Data loaded from config
    protected String name;
    protected int weight;
    protected int researchTime;

    protected final HashSet<TierRequirement> requirements = new HashSet<>();

    protected final List<NeoBlockSpec> blocks = new ArrayList<>();
    protected int totalBlockWeight = 0;

    protected final LinkedHashMap<Integer, NeoEventAction> onBlockActions = new LinkedHashMap<>();
    protected final LinkedHashMap<NeoEventBlockTrigger, NeoEventAction> otherBlockActions = new LinkedHashMap<>();

    public NeoTradePool trades;
    public NeoSeqBlockSpec startSequence;

    public NeoEventAction unlockActions;
    public NeoEventAction enableActions;
    public NeoEventAction disableActions;
    public NeoEventAction researchActions;

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

        int randomValue = WorldManager.getRandom().nextInt(totalBlockWeight);
        for (NeoBlockSpec entry: blocks) {
            randomValue -= entry.getWeight();
            if (randomValue < 0) return entry;
        }

        NeoBlock.LOGGER.error("Unable to get a random block from tier {}", id);
        return blocks.stream().findFirst().orElse(BlockManager.DEFAULT_SPEC);
    }
    public List<NeoBlockSpec> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }
    public NeoSeqBlockSpec getStartSequence() {
        return startSequence;
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
        return researched;
    }
    public boolean canBeResearched() {
        return canBeResearched(WorldManager.getInstance());
    }
    public boolean canBeResearched(WorldManager manager) {
        if (researched) return false;
        for (TierRequirement requirement: requirements)
            if (!requirement.isMet(manager, this)) return false;

        return true;
    }
    public void startResearch() {
        if (researched) return;
        WorldCooldown.Type.TierResearch.create(this);
    }
    public void setResearched(boolean value) {
        researched = value;
    }
    public int getResearchTime() {
        return researchTime;
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
        return researched && enabled;
    }
    public TierSpec enable() {
        enabled = true;
        enableActions.apply(WorldManager.getWorldLevel());
        return this;
    }
    public TierSpec disable() {
        enabled = false;
        disableActions.apply(WorldManager.getWorldLevel());
        return this;
    }

    public int setCount(int count) {
        this.count = count;
        for (Map.Entry<NeoEventBlockTrigger, NeoEventAction> entry: otherBlockActions.entrySet())
            if (entry.getKey().matches(count)) entry.getValue().apply(WorldManager.getWorldLevel());
        if (onBlockActions.containsKey(count)) onBlockActions.get(count).apply(WorldManager.getWorldLevel());
        return count;
    }
    public int getCount() {
        return count;
    }
    public int addCount(int count) {
        return setCount(this.count + count);
    }

    public int getID() {
        return id;
    }
}
