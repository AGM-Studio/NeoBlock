package xyz.agmstudio.neoblock.neo.merchants;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.*;

public class NeoMerchant {
    public static double chance;
    public static double increment;
    public static int attemptInterval;
    public static Range lifespan;

    public static void loadConfig() {
        CommentedFileConfig config = NeoBlockMod.getConfig();
        NeoMerchant.chance = config.get("neo-trader.chance");
        NeoMerchant.increment = config.get("neo-trader.chance-increment");
        NeoMerchant.attemptInterval = config.get("neo-trader.attempt-interval");
        NeoMerchant.lifespan = new Range(
                Math.max(0, config.get("neo-trader.life-span-min")),
                Math.max(0, config.get("neo-trader.life-span-max"))
        );

        NeoBlockMod.LOGGER.debug("NeoMerchant: Config loaded. \n\tChance: {}\n\tChance Increment: {}\n\tAttempt Interval: {}\n\tLifespan: {}", NeoMerchant.chance, NeoMerchant.increment, NeoMerchant.attemptInterval, NeoMerchant.lifespan);
    }

    protected final List<NeoOffer> trades;

    public static NeoMerchant parse(List<String> trades) {
        if (trades.isEmpty()) return null;

        NeoMerchant trader = new NeoMerchant();
        trades.stream().map(NeoOffer::parse).filter(Objects::nonNull).forEach(trader.trades::add);

        return trader;
    }
    public static @NotNull WanderingTrader spawnTraderWith(List<NeoOffer> trades, ServerLevel level) {
        NeoMerchant trader = new NeoMerchant();
        trader.trades.addAll(trades);

        return trader.spawnTrader(level, "NeoMerchant");
    }
    public static boolean exists(@NotNull ServerLevel level, String tag) {
        for (Entity entity: level.getEntities().getAll())
            if (entity.getTags().contains(tag)) return true;

        return false;
    }
    public static WanderingTrader attemptSpawnTrader(ServerLevel level) {
        if (WorldData.getBlockCount() % attemptInterval != 0 || exists(level, "NeoMerchant")) return null;
        double chance = NeoMerchant.chance + (increment * WorldData.getTraderFailedAttempts());
        if (WorldData.getRandom().nextFloat() > chance) {
            WorldData.addTraderFailedAttempts();
            NeoBlockMod.LOGGER.debug("Trader chance {} failed for {} times in a row", chance, WorldData.getTraderFailedAttempts());
            return null;
        }
        return forceSpawnTrader(level);
    }
    public static WanderingTrader forceSpawnTrader(ServerLevel level) {
        WorldData.resetTraderFailedAttempts();
        List<NeoOffer> trades = new ArrayList<>();
        WorldData.getTiers().stream().filter(WorldTier::isUnlocked)
                .forEach(tier -> trades.addAll(tier.getRandomTrades()));

        if (!trades.isEmpty()) {
            WanderingTrader trader = spawnTraderWith(trades, level);
            MinecraftUtil.Messenger.sendInstantMessage("message.neoblock.trader_spawned", level, true);

            HashMap<EntityType<?>, Integer> tradedMobs = WorldData.getTradedMobs();
            tradedMobs.forEach((type, count) -> {
                for (int i = 0; i < count; i++) {
                    Entity mob = type.spawn(level, trader.getOnPos(), MobSpawnType.SPAWN_EGG);
                    MinecraftUtil.Entities.leash(mob, trader);
                }
            });
            WorldData.clearTradedMobs();

            return trader;
        } return null;
    }

    public static void tick(@NotNull ServerLevel level) {
        for (Entity entity: level.getEntities().getAll())
            if (entity instanceof Villager villager
                    && villager.getTags().contains("NeoMerchant")
                    && villager.getPersistentData().getInt("NeoTradeLifespan") < villager.getAge()
            ) entity.remove(Entity.RemovalReason.DISCARDED);
    }

    protected NeoMerchant() {
        this.trades = new ArrayList<>();
    }

    public static final HashMap<UUID, MerchantOffers> offerMap = new HashMap<>();
    public WanderingTrader spawnTrader(ServerLevel level, String... tags) {
        WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, level);
        trader.setPos(WorldData.POS.getCenter().add(0, 2, 0));
        trader.setDespawnDelay(lifespan.get());
        for (String tag: tags) trader.addTag(tag);

        MerchantOffers offers = new MerchantOffers();
        this.trades.stream().map(NeoOffer::getOffer).forEach(offers::add);
        offerMap.put(trader.getUUID(), offers);

        level.addFreshEntity(trader);
        return trader;
    }
    public static void handleTrader(WanderingTrader trader) {
        MerchantOffers offers = offerMap.remove(trader.getUUID());
        if (offers == null) return;

        trader.getOffers().clear();
        offers.forEach(trader.getOffers()::add);
    }
}
