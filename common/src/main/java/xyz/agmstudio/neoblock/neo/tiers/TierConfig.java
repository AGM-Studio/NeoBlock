package xyz.agmstudio.neoblock.neo.tiers;

import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.block.NeoSeqBlockSpec;
import xyz.agmstudio.neoblock.neo.events.NeoEventAction;
import xyz.agmstudio.neoblock.neo.events.NeoEventBlockTrigger;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.platform.IConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

public class TierConfig {
    final String name;
    final int weight;
    final int researchTime;

    final HashSet<TierRequirement> requirements = new HashSet<>();

    final List<NeoBlockSpec> blocks = new ArrayList<>();
    final int totalBlockWeight;

    final LinkedHashMap<Integer, NeoEventAction> onBlockActions = new LinkedHashMap<>();
    final LinkedHashMap<NeoEventBlockTrigger, NeoEventAction> otherBlockActions = new LinkedHashMap<>();

    final NeoTradePool trades;
    final NeoSeqBlockSpec startSequence;

    final NeoEventAction unlockActions;
    final NeoEventAction enableActions;
    final NeoEventAction disableActions;
    final NeoEventAction researchActions;

    public TierConfig(@NotNull IConfig config) {
        this.name = config.get("name", "UNNAMED");
        NeoBlockMod.getLogger().debug("Loading tier {}...", this.name);

        this.researchTime = config.getInt("unlock.unlock-time", 0);
        long timeReq = config.getInt("unlock.game-time", -1);
        if (timeReq > 0) this.requirements.add(new TierRequirement.GameTime(timeReq));
        long blocksReq = config.getInt("unlock.blocks", -1);
        if (blocksReq > 0) this.requirements.add(new TierRequirement.BlockBroken(blocksReq));
        if (config.get("unlock.command", this.requirements.isEmpty()))
            this.requirements.add(new TierRequirement.Special());

        this.blocks.clear();
        final List<String> blocks_list = config.get("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(this.blocks::add));
        this.totalBlockWeight = this.blocks.stream().mapToInt(NeoBlockSpec::getWeight).sum();

        if (this.blocks.isEmpty()) this.weight = 0;
        else this.weight = Math.max(0, config.getInt("weight", 1));

        final List<String> list = config.get("trader-trades", config.get("trades", List.of()));
        this.trades = NeoTradePool.parse(list);

        final List<NeoBlockSpec> start = NeoSeqBlockSpec.extractSequenceList(config.get("starting-blocks", List.of()));
        this.startSequence = new NeoSeqBlockSpec(start, 1, "tier-" + this.name + "-start");

        this.unlockActions = new NeoEventAction(config, "on-unlock").withMessage("message.neoblock.unlocking_trader", this.name);
        this.enableActions = new NeoEventAction(config, "on-enable").withMessage("message.neoblock.enabling_trader", this.name);
        this.disableActions = new NeoEventAction(config, "on-disable").withMessage("message.neoblock.disabling_trader", this.name);
        this.researchActions = new NeoEventAction(config, "on-research").withMessage("message.neoblock.research_trader", this.name);

        for (String key: config.keys()) {
            Matcher obm = NeoEventBlockTrigger.ON_BLOCK_PATTERN.matcher(key);
            if (obm.matches()) {
                int count = Integer.parseInt(obm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, obm.group()).withMessage("message.neoblock.trader_spawned", this.name);
                this.onBlockActions.put(count, actions);
                NeoBlockMod.getLogger().debug("Added OB {} action for tier {}.", key, this.name);
            }
            Matcher ebm = NeoEventBlockTrigger.EVERY_BLOCK_PATTERN.matcher(key);
            if (ebm.matches()) {
                int count = Integer.parseInt(ebm.group("count"));
                NeoEventAction actions = new NeoEventAction(config, ebm.group()).withMessage("message.neoblock.trader_spawned", this.name);
                this.otherBlockActions.put(new NeoEventBlockTrigger.Every(count), actions);
                NeoBlockMod.getLogger().debug("Added EB {} action for tier {}.", key, this.name);
            }
            Matcher ebo = NeoEventBlockTrigger.EVERY_BLOCK_OFFSET_PATTERN.matcher(key);
            if (ebo.matches()) {
                int count = Integer.parseInt(ebo.group("count"));
                int offset = Integer.parseInt(ebo.group("offset"));
                NeoEventAction actions = new NeoEventAction(config, ebo.group()).withMessage("message.neoblock.trader_spawned", this.name);
                this.otherBlockActions.put(new NeoEventBlockTrigger.EveryOffset(count, offset), actions);
                NeoBlockMod.getLogger().debug("Added EBO {} action for tier {}.", key, this.name);
            }
        }

        NeoBlockMod.getLogger().debug("Tier {} loaded.", this.name);
    }

    public NeoBlockSpec getRandomBlock() {
        if (blocks.isEmpty()) return NeoBlock.DEFAULT_SPEC;

        int randomValue = WorldManager.getRandom().nextInt(totalBlockWeight);
        for (NeoBlockSpec entry: blocks) {
            randomValue -= entry.getWeight();
            if (randomValue < 0) return entry;
        }

        NeoBlockMod.getLogger().error("Unable to get a random block from tier {}", name);
        return blocks.stream().findFirst().orElse(NeoBlock.DEFAULT_SPEC);
    }

    /**
     * Loads all available tier configuration files from resources if they do not exist.
     */
    public static void loadAllTierConfigs() {
        // Prioritize template config
        Path templateLocation = TierSpec.FOLDER.resolve("tier-template.toml");
        if (!Files.exists(templateLocation)) {
            try {
                NeoBlockMod.get().processResourceFile("/configs/tiers/tier-template.toml", templateLocation, Map.of("[TIER]", "10"));
                NeoBlockMod.getLogger().debug("Loaded tier template config.");
            } catch (IOException e) {
                NeoBlockMod.getLogger().error("Unable to process tier template resource", e);
            }
        }

        // If tier-0.toml is present, no need to proceed
        if (Files.exists(TierSpec.FOLDER.resolve("tier-0.toml"))) return;
        if (TierSpec.FOLDER.toFile().mkdirs())
            NeoBlockMod.getLogger().debug("Created config folder: {}", TierSpec.FOLDER);

        int tier = 0;
        while (true) {
            Path location = TierSpec.FOLDER.resolve("tier-" + tier + ".toml");
            String resource = "/configs/tiers/tier-" + tier + ".toml";
            Map<String, String> map = Map.of("[TIER]", Integer.toString(tier ++));

            if (Files.exists(location)) continue;
            if (!NeoBlockMod.get().doesResourceExist(resource)) break;

            try {
                NeoBlockMod.get().processResourceFile(resource, location, map);
                NeoBlockMod.getLogger().debug("Loaded tier config from resource: {}", resource);
            } catch (IOException e) {
                NeoBlockMod.getLogger().error("Unable to process resource {}", resource, e);
                break;
            }
        }
    }
}