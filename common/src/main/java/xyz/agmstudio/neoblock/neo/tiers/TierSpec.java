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
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.PatternUtil;
import xyz.agmstudio.neoblock.util.ResourceUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

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

        if (loadConfig) this.loadConfig();

        this.hash = getHashCode();
    }

    @Override
    public void onLoad(CompoundTag tag) {
        this.loadConfig();
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

    public void loadConfig() {
        Path FOLDER = ResourceUtil.getConfigFolder(NeoBlock.MOD_ID, "tiers");
        IConfig config = IConfig.getConfig(FOLDER, "tier-" + this.id);
        if (config == null) throw new NBTSaveable.AbortException("Unable to find config for tier " + this.id);

        NeoBlock.LOGGER.debug("Loading tier {}...", this.id);
        this.name = config.get("name", "Tier-" + this.id);

        this.requirements.clear();
        this.researchTime = config.getInt("unlock.unlock-time", 0);
        if (this.id > 0) {
            long time = config.getInt("unlock.game-time", -1);
            if (time > 0) this.requirements.add(new TierRequirement.GameTime(time));
            long blocks = config.getInt("unlock.blocks", -1);
            if (blocks > 0) this.requirements.add(new TierRequirement.BlockBroken(blocks));
            if (config.get("unlock.command", this.requirements.isEmpty()))
                this.requirements.add(new TierRequirement.Special());
        } else this.researched = true;

        this.blocks.clear();
        final List<String> blocks_list = config.get("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(this.blocks::add));
        this.totalBlockWeight = blocks.stream().mapToInt(NeoBlockSpec::getWeight).sum();

        if (this.blocks.isEmpty()) this.weight = 0;
        else this.weight = Math.max(0, config.getInt("weight", 1));

        final List<String> list = config.get("trader-trades", config.get("trades", List.of()));
        this.trades = NeoTradePool.parse(list);

        final List<NeoBlockSpec> start = NeoSeqBlockSpec.extractSequenceList(config.get("starting-blocks", List.of()));
        this.startSequence = new NeoSeqBlockSpec(start, 1, "tier-" + this.id + "-start");

        this.unlockActions = new NeoEventAction(config, "on-unlock").withMessage("message.neoblock.unlocking_trader", this.id);
        this.enableActions = new NeoEventAction(config, "on-enable").withMessage("message.neoblock.enabling_trader", this.id);
        this.disableActions = new NeoEventAction(config, "on-disable").withMessage("message.neoblock.disabling_trader", this.id);
        this.researchActions = new NeoEventAction(config, "on-research").withMessage("message.neoblock.research_trader", this.id);

        for (String key: config.keys()) {
            Matcher obm = PatternUtil.ON_BLOCK_PATTERN.matcher(key);
            if (obm.matches()) {
                int count = Integer.parseInt(obm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, obm.group()).withMessage("message.neoblock.trader_spawned", this.id);
                this.onBlockActions.put(count, actions);
                NeoBlock.LOGGER.debug("Added OB {} action for tier {}.", key, this.id);
            }
            Matcher ebm = PatternUtil.EVERY_BLOCK_PATTERN.matcher(key);
            if (ebm.matches()) {
                int count = Integer.parseInt(ebm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, ebm.group()).withMessage("message.neoblock.trader_spawned", this.id);
                this.otherBlockActions.put(new NeoEventBlockTrigger.Every(count), actions);
                NeoBlock.LOGGER.debug("Added EB {} action for tier {}.", key, this.id);
            }
            Matcher ebo = PatternUtil.EVERY_BLOCK_OFFSET_PATTERN.matcher(key);
            if (ebo.matches()) {
                int count = Integer.parseInt(ebo.group("count"));
                int offset = Integer.parseInt(ebo.group("offset"));
                NeoEventAction actions = new NeoEventAction(config, ebo.group()).withMessage("message.neoblock.trader_spawned", this.id);
                this.otherBlockActions.put(new NeoEventBlockTrigger.EveryOffset(count, offset), actions);
                NeoBlock.LOGGER.debug("Added EBO {} action for tier {}.", key, this.id);
            }
        }

        NeoBlock.LOGGER.debug("Tier {} loaded. Hash key: {}", this.id, this.getHashCode());
    }

    // Methods
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
