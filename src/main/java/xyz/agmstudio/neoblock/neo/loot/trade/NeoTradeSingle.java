package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.loot.NeoItemStack;
import xyz.agmstudio.neoblock.neo.loot.NeoParser;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTradeSingle extends NeoTrade {
    private static final Pattern PATTERN = Pattern.compile(
            "\\s*(?<result>[^;]+?)\\s*;" +
                    "\\s*(?<costA>[^;]+?)\\s*;" +
                    "(?:\\s*(?<costB>[^;]+?)\\s*;)?" +
                    "(?:\\s*(?<uses>\\d+(?:-\\d+)?)\\s*)?" +
                    "(?:\\s*(?<chance>\\d+(?:\\.\\d+)?)%\\s*)?"
    );

    private final NeoItemStack result;
    private final NeoItemStack costA;
    private final @Nullable NeoItemStack costB;
    private final double chance;
    private final UniformInt uses;

    public NeoTradeSingle(NeoItemStack result, NeoItemStack costA, @Nullable NeoItemStack costB, double chance, UniformInt uses) {
        this.result = result;
        this.costA = costA;
        this.costB = costB;
        this.chance = Math.min(Math.max(chance, 0.0), 1.0);
        this.uses = uses == null ? UniformInt.of(1, 1) : uses;
    }

    @Override public Optional<MerchantOffer> getOffer() {
        if (WorldData.getRandom().nextDouble() > chance) return Optional.empty();
        return MinecraftUtil.Merchant.getOfferOf(result, costA, costB, uses);
    }

    public static Optional<NeoTrade> parse(String input) {
        if (input == null) return Optional.empty();

        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        NeoItemStack result = NeoItemStack.parseItem(matcher.group("result")).orElse(null);
        NeoItemStack costA = NeoItemStack.parseItem(matcher.group("costA")).orElse(null);
        if (result == null || costA == null) {
            String key = result == null ? "result" : "costA";
            NeoBlockMod.LOGGER.error("Invalid trade {} '{}' for: {}", key, matcher.group(key), input);
            return Optional.empty();
        }

        NeoItemStack costB = NeoItemStack.parseItem(matcher.group("costB")).orElse(null);
        double chance = NeoParser.parseChance(matcher.group("chance"));
        UniformInt uses = NeoParser.parseRange(matcher.group("uses"));

        return Optional.of(new NeoTradeSingle(result, costA, costB, chance, uses));
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(result.toString()).append("; ")
                .append(costA.toString()).append("; ");
        if (costB != null) sb.append(costB).append(";");
        sb.append(NeoParser.stringChance(chance));
        sb.append(" ").append(NeoParser.stringUniformInt(uses));
        return sb.toString();
    }
}
