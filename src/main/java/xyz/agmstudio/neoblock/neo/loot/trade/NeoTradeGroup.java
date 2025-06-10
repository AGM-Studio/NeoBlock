package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.neo.loot.NeoParser;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTradeGroup extends NeoTrade {
    private static final Pattern PATTERN = Pattern.compile("trade:(?<name>[\\w\\-]+)(?:\\s+(?<chance>\\d+\\.?\\d*)%?)?");

    private final String name;
    private final double chance;

    public NeoTradeGroup(String name, double chance) {
        this.name = name;
        this.chance = Math.min(Math.max(chance, 0.0), 1.0);
    }

    @Override
    public Optional<MerchantOffer> getOffer() {
        if (WorldData.getRandom().nextDouble() > chance) return Optional.empty();

        List<NeoTrade> trades = NeoTrade.getTrades(name);
        if (trades == null || trades.isEmpty()) return Optional.empty();

        NeoTrade selected = trades.get(WorldData.getRandom().nextInt(trades.size()));
        return selected.getOffer();
    }

    public static Optional<NeoTrade> parse(String input) {
        if (input == null) return Optional.empty();

        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        String name = matcher.group("name");
        double chance = NeoParser.parseChance(matcher.group("chance"));

        return Optional.of(new NeoTradeGroup(name, chance));
    }

    @Override
    public String toString() {
        return "trade:" + name + NeoParser.stringChance(chance);
    }
}

