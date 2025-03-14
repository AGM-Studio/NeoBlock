package xyz.agmstudio.neoblock.tiers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import xyz.agmstudio.neoblock.util.Range;

import java.util.ArrayList;
import java.util.List;

public class NeoTrader {

    protected final List<NeoTrade> trades;

    public static float chance;
    public static float increment;
    public static int attemptInterval;
    public static Range lifespan;

    public static NeoTrader parse(List<String> trades) {
        if (trades.isEmpty()) return null;

        NeoTrader trader = new NeoTrader();
        trades.stream().map(NeoTrade::parse).forEach(trader.trades::add);

        return trader;
    }
    public static void spawnWanderingTraderWith(List<NeoTrade> trades, Level level, LivingEntity... leaches) {
        NeoTrader trader = new NeoTrader();
        trader.trades.addAll(trades);
        trader.spawnWanderingTrader(level, "NeoTrader", leaches);
    }

    protected NeoTrader() {
        this.trades = new ArrayList<>();
    }


    public Villager spawnTrader(Level level) {
        return spawnTrader(level, null);
    }
    public Villager spawnTrader(Level level, String tag) {
        Villager trader = new Villager(EntityType.VILLAGER, level);
        trader.setPos(NeoBlock.POS.getCenter().add(0, 2, 0));
        trader.setVillagerData(trader.getVillagerData()
                .setProfession(VillagerProfession.NITWIT)
                .setLevel(5)
        );
        if (tag != null) trader.addTag(tag);

        MerchantOffers offers = new MerchantOffers();
        this.trades.stream().map(NeoTrade::getOffer).forEach(offers::add);
        trader.setOffers(offers);

        level.addFreshEntity(trader);
        return trader;
    }

    public WanderingTrader spawnWanderingTrader(Level level, LivingEntity... leashed) {
        return spawnWanderingTrader(level, null, leashed);
    }
    public WanderingTrader spawnWanderingTrader(Level level, String tag, LivingEntity... leashed) {
        WanderingTrader trader = new WanderingTrader(EntityType.WANDERING_TRADER, level);
        trader.setPos(NeoBlock.POS.getCenter().add(0, 2, 0));
        if (tag != null) trader.addTag(tag);

        MerchantOffers offers = new MerchantOffers();
        this.trades.stream().map(NeoTrade::getOffer).forEach(offers::add);
        trader.overrideOffers(offers);

        level.addFreshEntity(trader);
        for (LivingEntity mob: leashed) {
            level.addFreshEntity(mob);
            if (mob instanceof Leashable leashable)
                leashable.setLeashedTo(trader, true);
        }

        return trader;
    }
}
