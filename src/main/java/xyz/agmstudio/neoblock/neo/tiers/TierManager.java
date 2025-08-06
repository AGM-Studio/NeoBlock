package xyz.agmstudio.neoblock.neo.tiers;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.block.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.List;

public class TierManager {
    public static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID + "/tiers");

    protected static void loadConfig(final TierSpec spec) {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "tier-" + spec.id);
        if (config == null) throw new RuntimeException("Unable to find config for tier " + spec.id);

        NeoBlockMod.LOGGER.debug("Loading tier {}...", spec.id);
        spec.name = config.getOrElse("name", "Tier-" + spec.id);

        final UnmodifiableConfig lockConfig = config.get("unlock");
        spec.research.time = config.getIntOrElse("unlock-time", 0);
        if (spec.id > 0) {
            long time = config.getIntOrElse("game-time", -1);
            if (time > 0) spec.requirements.add(new TierRequirement.GameTime(time));
            long blocks = config.getIntOrElse("blocks", -1);
            if (blocks > 0) spec.requirements.add(new TierRequirement.BlockBroken(blocks));
            if (config.getOrElse("command", spec.requirements.isEmpty()))
                spec.requirements.add(new TierRequirement.Special());
        }

        final List<String> blocks_list = config.getOrElse("blocks", List.of("minecraft:grass_block"));
        blocks_list.forEach(value -> NeoBlockSpec.parse(value).ifPresent(spec.blocks::add));

        if (spec.blocks.isEmpty()) spec.weight = 0;
        else spec.weight = Math.max(0, config.getIntOrElse("weight", 1));

        final List<String> unlockTrades = config.getOrElse("unlock-trades", List.of());
        spec.tradePoolUnlock = NeoTradePool.parse(unlockTrades);

        final List<String> list = config.getOrElse("trader-trades", config.getOrElse("trades", List.of()));
        spec.trades = NeoTradePool.parse(list);

        NeoBlockMod.LOGGER.debug("Tier {} loaded. Hash key: {}", spec.id, spec.getHashCode());
    }
}
