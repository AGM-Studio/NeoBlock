package xyz.agmstudio.neoblock.neo.loot.trade;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

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

    private final NeoItemSpec result;
    private final NeoItemSpec costA;
    private final @Nullable NeoItemSpec costB;
    private final double chance;
    private final UniformInt uses;

    public NeoTradeSingle(NeoItemSpec result, NeoItemSpec costA, @Nullable NeoItemSpec costB, double chance, UniformInt uses) {
        this.result = result;
        this.costA = costA;
        this.costB = costB;
        this.chance = Math.min(Math.max(chance, 0.0), 1.0);
        this.uses = uses == null ? UniformInt.of(1, 1) : uses;
    }

    @Override public Optional<MerchantOffer> getOffer() {
        if (WorldData.getRandom().nextDouble() > chance) return Optional.empty();
        return MinecraftUtil.getOfferOf(result, costA, costB, uses);
    }

    public static Optional<NeoTrade> parse(String input) {
        if (input == null) return Optional.empty();

        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        NeoItemSpec result = NeoItemSpec.parseItem(matcher.group("result")).orElse(null);
        NeoItemSpec costA = NeoItemSpec.parseItem(matcher.group("costA")).orElse(null);
        if (result == null || costA == null) {
            String key = result == null ? "result" : "costA";
            NeoBlock.LOGGER.error("Invalid trade {} '{}' for: {}", key, matcher.group(key), input);
            return Optional.empty();
        }

        NeoItemSpec costB = NeoItemSpec.parseItem(matcher.group("costB")).orElse(null);
        double chance = StringUtil.parseChance(matcher.group("chance"));
        UniformInt uses = StringUtil.parseRange(matcher.group("uses"));

        return Optional.of(new NeoTradeSingle(result, costA, costB, chance, uses));
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(result.toString()).append("; ")
                .append(costA.toString()).append("; ");
        if (costB != null) sb.append(costB).append(";");
        sb.append(StringUtil.stringChance(chance));
        sb.append(" ").append(StringUtil.stringUniformInt(uses));
        return sb.toString();
    }
}
