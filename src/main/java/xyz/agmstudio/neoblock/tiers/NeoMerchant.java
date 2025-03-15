package xyz.agmstudio.neoblock.tiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.util.ArrayList;
import java.util.List;
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
    public static @NotNull Villager spawnTraderWith(List<NeoOffer> trades, ServerLevel level) {
        NeoMerchant trader = new NeoMerchant();
        trader.trades.addAll(trades);

        Villager villager = trader.spawnTrader(level, "NeoMerchant");
        villager.getPersistentData().putInt("NeoTradeLifespan", lifespan.get());
        return villager;
    }
    public static boolean exists(@NotNull ServerLevel level, String tag) {
        for (Entity entity: level.getEntities().getAll())
            if (entity.getTags().contains(tag)) return true;

        return false;
    }
    public static void attemptSpawnTrader(ServerLevel level) {
        int breaks = NeoBlock.DATA.getBlockCount();
        if (breaks % attemptInterval != 0 || exists(level, "NeoMerchant")) return;
        float chance = NeoMerchant.chance + (increment * NeoBlock.DATA.getTraderFailedAttempts());
        if (RandomGenerator.getDefault().nextFloat() > chance) {
            NeoBlock.DATA.addTraderFailedAttempts();
            NeoBlockMod.LOGGER.debug("Trader chance {} failed for {} times in a row", chance, NeoBlock.DATA.getTraderFailedAttempts());
            return;
        }
        NeoBlock.DATA.resetTraderFailedAttempts();
        List<NeoOffer> trades = new ArrayList<>();
        NeoBlock.TIERS.stream().filter(NeoTier::isUnlocked)
                .forEach(tier -> trades.addAll(tier.getRandomTrades()));

        if (!trades.isEmpty()) {
            Villager trader = spawnTraderWith(trades, level);
            MessagingUtil.sendInstantMessage("message.neoblock.trader_spawned", level, true);
        }
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
        this.trades.stream().map(NeoOffer::getOffer).forEach(offers::add);
        trader.setOffers(offers);
        return trader;
    }
}
