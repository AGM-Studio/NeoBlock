package xyz.agmstudio.neoblock.tiers.merchants;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.NeoTier;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

public class NeoMerchant {

    protected final List<NeoOffer> trades;

    public static float chance;
    public static float increment;
    public static int attemptInterval;
    public static Range lifespan;

    public static NeoMerchant parse(List<String> trades) {
        if (trades.isEmpty()) return null;

        NeoMerchant trader = new NeoMerchant();
        trades.stream().map(NeoOffer::parse).forEach(trader.trades::add);

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
        if (NeoBlock.DATA.getBlockCount() % attemptInterval != 0 || exists(level, "NeoMerchant")) return null;
        float chance = NeoMerchant.chance + (increment * NeoBlock.DATA.getTraderFailedAttempts());
        if (RandomGenerator.getDefault().nextFloat() > chance) {
            NeoBlock.DATA.addTraderFailedAttempts();
            NeoBlockMod.LOGGER.debug("Trader chance {} failed for {} times in a row", chance, NeoBlock.DATA.getTraderFailedAttempts());
            return null;
        }
        return forceSpawnTrader(level);
    }
    public static WanderingTrader forceSpawnTrader(ServerLevel level) {
        NeoBlock.DATA.resetTraderFailedAttempts();
        List<NeoOffer> trades = new ArrayList<>();
        NeoBlock.TIERS.stream().filter(NeoTier::isUnlocked)
                .forEach(tier -> trades.addAll(tier.getRandomTrades()));

        if (!trades.isEmpty()) {
            WanderingTrader trader = spawnTraderWith(trades, level);
            MessagingUtil.sendInstantMessage("message.neoblock.trader_spawned", level, true);

            HashMap<EntityType<?>, Integer> tradedMobs = NeoBlock.DATA.getTradedMobs();
            tradedMobs.forEach((type, count) -> {
                for (int i = 0; i < count; i++) {
                    Entity mob = type.spawn(level, trader.getOnPos(), MobSpawnType.SPAWN_EGG);
                    if (mob instanceof Leashable leashable) leashable.setLeashedTo(trader, true);
                }
            });
            tradedMobs.clear();

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
        trader.setPos(NeoBlock.POS.getCenter().add(0, 2, 0));
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
