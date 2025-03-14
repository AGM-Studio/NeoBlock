package xyz.agmstudio.neoblock.tiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
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
    public static @NotNull Villager spawnTraderWith(List<NeoTrade> trades, ServerLevel level) {
        NeoTrader trader = new NeoTrader();
        trader.trades.addAll(trades);

        Villager villager = trader.spawnTrader(level, "NeoTrader");
        villager.getPersistentData().putInt("NeoTradeLifespan", lifespan.get());
        return villager;
    }
    public static boolean exists(@NotNull ServerLevel level, String tag) {
        for (Entity entity: level.getEntities().getAll())
            if (entity.getTags().contains(tag)) return true;

        return false;
    }

    public static void manageTraders(@NotNull ServerLevel level) {
        for (Entity entity: level.getEntities().getAll())
            if (entity instanceof Villager villager
                    && villager.getTags().contains("NeoTrader")
                    && villager.getPersistentData().getInt("NeoTradeLifespan") < villager.getAge()
            ) entity.remove(Entity.RemovalReason.DISCARDED);
    }

    protected NeoTrader() {
        this.trades = new ArrayList<>();
    }

    public Villager spawnTrader(ServerLevel level) {
        return spawnTrader(level, null);
    }
    public Villager spawnTrader(ServerLevel level, String tag) {
        Villager trader = new Villager(EntityType.VILLAGER, level);
        trader.setPos(NeoBlock.POS.getCenter().add(0, 2, 0));
        trader.setVillagerData(trader.getVillagerData()
                .setProfession(VillagerProfession.NITWIT)
                .setLevel(5)
        );

        if (tag != null) {
            for (Entity entity: level.getEntities().getAll())
                if (entity.getTags().contains(tag)) entity.remove(Entity.RemovalReason.DISCARDED);

            trader.addTag(tag);
        }
        level.addFreshEntity(trader);

        MerchantOffers offers = new MerchantOffers();
        this.trades.stream().map(NeoTrade::getOffer).forEach(offers::add);
        trader.setOffers(offers);
        return trader;
    }
}
