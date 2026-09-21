package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.platform.IConfig;
import xyz.agmstudio.neocore.NeoMC;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


@ParametersAreNonnullByDefault
public class NeoMerchant {
    public static double chance;
    public static double increment;
    public static int attemptInterval;
    public static UniformInt lifespan;

    public static void loadConfig() {
        IConfig config = NeoBlockMod.getConfig();
        NeoMerchant.chance = config.get("neo-trader.chance");
        NeoMerchant.increment = config.get("neo-trader.chance-increment");
        NeoMerchant.attemptInterval = config.get("neo-trader.attempt-interval");
        NeoMerchant.lifespan = UniformInt.of(
                Math.max(0, config.get("neo-trader.life-span-min")),
                Math.max(0, config.get("neo-trader.life-span-max"))
        );

        NeoBlockMod.getLogger().debug("NeoMerchant: Config loaded. \n\tChance: {}\n\tChance Increment: {}\n\tAttempt Interval: {}\n\tLifespan: {}", NeoMerchant.chance, NeoMerchant.increment, NeoMerchant.attemptInterval, NeoMerchant.lifespan);
    }

    public static @Nullable WanderingTrader spawnTraderWith(List<NeoTrade> trades, NeoBlock at, String... tags) {
        NeoMerchant trader = new NeoMerchant();
        trader.trades.addAll(trades);

        if (trader.trades.isEmpty()) return null;
        return trader.spawnTrader(at, tags);
    }
    public static boolean exists(@NotNull ServerLevel level, String tag) {
        for (Entity entity: NeoMC.allEntities(level))
            if (entity.getTags().contains(tag)) return true;

        return false;
    }
    public static @Nullable WanderingTrader attemptSpawnTrader(NeoBlock at) {
        if (at.getBlockCount() % attemptInterval != 0 || exists(at.level, "NeoMerchant")) return null;
        double chance = NeoMerchant.chance + (increment * at.getTraderFailedAttempts());
        if (WorldManager.getRandom().nextFloat() > chance) {
            int fails = at.addTraderFailedAttempts();
            NeoBlockMod.getLogger().debug("Trader chance {} failed for {} times in a row", chance, fails);
            return null;
        }
        return forceSpawnTrader(at);
    }
    public static @Nullable WanderingTrader forceSpawnTrader(NeoBlock at) {
        at.resetTraderFailedAttempts();
        List<NeoTrade> trades = new ArrayList<>();
        at.getTierStream().filter(TierSpec::isEnabled).forEach(tier -> trades.addAll(tier.getTrades()));

        WanderingTrader trader = spawnTraderWith(trades, at, "NeoMerchant");
        if (trader == null) return null;

        NeoBlockMod.sendInstantMessage("message.neoblock.trader_spawned", at.level, true);

        HashMap<EntityType<?>, Integer> tradedMobs = at.getTradedMobs();
        tradedMobs.forEach((type, count) -> {
            for (int i = 0; i < count; i++) {
                Entity mob = NeoMC.spawnEntity(at.level, type, trader.getOnPos());
                NeoMC.leash(mob, trader);
            }
        });
        at.clearTradedMobs();

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
    public WanderingTrader spawnTrader(NeoBlock at, String... tags) {
        WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, at.level);
        trader.setPos(at.safeBlock().above(2).getCenter());
        trader.setDespawnDelay(lifespan.sample(WorldManager.getRandom()));
        for (String tag: tags) trader.addTag(tag);

        MerchantOffers offers = new MerchantOffers();
        for (NeoTrade trade: trades) trade.getOffer(at).ifPresent(offers::add);
        offerMap.put(trader.getUUID(), offers);

        at.level.addFreshEntity(trader);
        return trader;
    }
}