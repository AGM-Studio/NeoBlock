package xyz.agmstudio.neoblock.neo.loot.trade;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.EntityAPI;
import xyz.agmstudio.neoblock.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldStatus;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

import java.util.*;

public class NeoMerchant {
    public static double chance;
    public static double increment;
    public static int attemptInterval;
    public static UniformInt lifespan;

    public static void loadConfig() {
        CommentedFileConfig config = NeoBlockMod.getConfig();
        NeoMerchant.chance = config.get("neo-trader.chance");
        NeoMerchant.increment = config.get("neo-trader.chance-increment");
        NeoMerchant.attemptInterval = config.get("neo-trader.attempt-interval");
        NeoMerchant.lifespan = UniformInt.of(
                Math.max(0, config.get("neo-trader.life-span-min")),
                Math.max(0, config.get("neo-trader.life-span-max"))
        );

        NeoBlockMod.LOGGER.debug("NeoMerchant: Config loaded. \n\tChance: {}\n\tChance Increment: {}\n\tAttempt Interval: {}\n\tLifespan: {}", NeoMerchant.chance, NeoMerchant.increment, NeoMerchant.attemptInterval, NeoMerchant.lifespan);
    }

    public static @Nullable WanderingTrader spawnTraderWith(List<NeoTrade> trades, ServerLevel level, String... tags) {
        NeoMerchant trader = new NeoMerchant();
        trader.trades.addAll(trades);

        if (trader.trades.isEmpty()) return null;
        return trader.spawnTrader(level, tags);
    }
    public static boolean exists(@NotNull ServerLevel level, String tag) {
        for (Entity entity: level.getEntities().getAll())
            if (entity.getTags().contains(tag)) return true;

        return false;
    }
    public static WanderingTrader attemptSpawnTrader(ServerLevel level) {
        WorldStatus status = WorldData.getWorldStatus();
        if (status.getBlockCount() % attemptInterval != 0 || exists(level, "NeoMerchant")) return null;
        double chance = NeoMerchant.chance + (increment * status.getTraderFailedAttempts());
        if (WorldData.getRandom().nextFloat() > chance) {
            int fails = status.addTraderFailedAttempts();
            NeoBlockMod.LOGGER.debug("Trader chance {} failed for {} times in a row", chance, fails);
            return null;
        }
        return forceSpawnTrader(level);
    }
    public static WanderingTrader forceSpawnTrader(ServerLevel level) {
        WorldStatus status = WorldData.getWorldStatus();
        status.resetTraderFailedAttempts();
        List<NeoTrade> trades = new ArrayList<>();
        WorldData.getWorldTiers().stream().filter(WorldTier::isEnabled).forEach(tier -> trades.addAll(tier.getTrades()));

        WanderingTrader trader = spawnTraderWith(trades, level, "NeoMerchant");
        if (trader == null) return null;

        MessengerAPI.sendInstantMessage("message.neoblock.trader_spawned", level, true);

        HashMap<EntityType<?>, Integer> tradedMobs = status.getTradedMobs();
        tradedMobs.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                Entity mob = type.spawn(level, trader.getOnPos(), MobSpawnType.SPAWN_EGG);
                EntityAPI.leash(mob, trader);
            }
        });
        status.clearTradedMobs();

        return trader;
    }

    public static final HashMap<UUID, MerchantOffers> offerMap = new HashMap<>();

    public static void handleTrader(WanderingTrader trader) {
        MerchantOffers offers = offerMap.remove(trader.getUUID());
        if (offers == null) return;

        trader.getOffers().clear();
        offers.forEach(trader.getOffers()::add);
    }

    private final List<NeoTrade> trades = new ArrayList<>();
    public WanderingTrader spawnTrader(ServerLevel level, String... tags) {
        WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, level);
        trader.setPos(BlockManager.POS.getCenter().add(0, 2, 0));
        trader.setDespawnDelay(lifespan.sample(WorldData.getRandom()));
        for (String tag: tags) trader.addTag(tag);

        MerchantOffers offers = new MerchantOffers();
        for (NeoTrade trade: trades) trade.getOffer().ifPresent(offers::add);
        offerMap.put(trader.getUUID(), offers);

        level.addFreshEntity(trader);
        return trader;
    }
}
