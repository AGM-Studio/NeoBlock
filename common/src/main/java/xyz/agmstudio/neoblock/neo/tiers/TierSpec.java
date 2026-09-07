package xyz.agmstudio.neoblock.neo.tiers;

import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTrade;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neocore.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.events.NeoEventAction;
import xyz.agmstudio.neoblock.neo.events.NeoEventBlockTrigger;
import xyz.agmstudio.neoblock.neo.world.NeoBlockCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldManager;

import java.nio.file.Path;
import java.util.*;

public class TierSpec implements NBTSaveable {
    public static final Path FOLDER = NeoBlockMod.get().getConfigFolder("tiers");

    public final NeoBlock block;
    private final String id;
    private final TierConfig config;

    // Stored data in world info
    @NBTData protected int count = 0;
    @NBTData protected boolean enabled;
    @NBTData protected boolean commanded = false;
    @NBTData protected boolean researched = false;

    public TierSpec(@NotNull NeoBlock block, @NotNull String id, @NotNull TierConfig config) {
        this.id = id;
        this.config = config;
        this.block = block;
    }

    public NeoBlockSpec getRandomBlock() {
        return config.getRandomBlock();
    }
    public List<NeoBlockSpec> getBlocks() {
        return Collections.unmodifiableList(config.blocks);
    }
    public NeoSeqBlockSpec getStartSequence() {
        return config.startSequence;
    }
    public double getTotalBlockWeight() {
        return config.totalBlockWeight;
    }

    public @NotNull String getName() {
        return config.name;
    }
    public int getWeight() {
        return config.weight;
    }

    public boolean isResearched() {
        return researched;
    }
    public boolean canBeResearched() {
        return canBeResearched(WorldManager.get());
    }
    public boolean canBeResearched(WorldManager manager) {
        if (researched) return false;
        for (TierRequirement requirement: config.requirements)
            if (!requirement.isMet(this)) return false;

        return true;
    }
    public void startResearch() {
        if (researched) return;
        NeoBlockCooldown.Type.TierResearch.create(this);
    }
    public void setResearched(boolean value) {
        researched = value;
    }
    public int getResearchTime() {
        return config.researchTime;
    }

    public void setSpecialRequirement(boolean special) {
        this.commanded = special;
    }
    public Set<TierRequirement> getRequirements() {
        return Collections.unmodifiableSet(config.requirements);
    }
    public boolean hasSpecialRequirement() {
        for (TierRequirement requirement: config.requirements)
            if (requirement instanceof TierRequirement.Special) return true;

        return false;
    }


    public NeoEventAction getDisableActions() {
        return config.disableActions;
    }
    public NeoEventAction getEnableActions() {
        return config.enableActions;
    }
    public NeoEventAction getResearchActions() {
        return config.researchActions;
    }
    public NeoEventAction getUnlockActions() {
        return config.unlockActions;
    }

    public boolean isEnabled() {
        return researched && enabled;
    }
    public TierSpec enable() {
        enabled = true;
        config.enableActions.apply(block);
        return this;
    }
    public TierSpec disable() {
        enabled = false;
        config.disableActions.apply(block);
        return this;
    }

    public int setCount(int count) {
        this.count = count;
        for (Map.Entry<NeoEventBlockTrigger, NeoEventAction> entry: config.otherBlockActions.entrySet())
            if (entry.getKey().matches(count)) entry.getValue().apply(block);
        if (config.onBlockActions.containsKey(count)) config.onBlockActions.get(count).apply(block);
        return count;
    }
    public int getCount() {
        return count;
    }
    public int addCount(int count) {
        return setCount(this.count + count);
    }

    public String getID() {
        return id;
    }

    public Collection<? extends NeoTrade> getTrades() {
        return config.trades.getPool();
    }
}