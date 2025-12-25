package xyz.agmstudio.neoblock.neo.tiers;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldRules;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TierSpecActions {
    public interface BlockTrigger {
        boolean matches(int count);
        record Every(int n) implements BlockTrigger {
            @Override public boolean matches(int count) {
                return count % n == 0;
            }
        }
        record EveryOffset(int n, int offset) implements BlockTrigger {
            @Override public boolean matches(int count) {
                if (count < n) return false;
                return (count - offset) % n == 0;
            }
        }
    }

    private final List<String> commands;
    private final List<Component> messages;
    private final Component actionMessage;
    private final Component customTraderMessage;
    private final NeoTradePool trades;
    private final HashMap<String, Object> rules;

    private String traderMessage = null;
    private Object[] traderMessageArgs = new Object[] {};

    public TierSpecActions(@NotNull IConfig config, String type) {
        this.commands = config.get(type + ".commands", List.of());
        List<String> messages = config.get(type + ".messages", List.of());
        this.messages = messages.stream().map(StringUtil::parseMessage).collect(Collectors.toList());
        String actionMessage = config.get(type + ".action-message", "");
        this.actionMessage = StringUtil.parseMessage(actionMessage);
        String traderMessage = config.get(type + ".trader-message", "");
        this.customTraderMessage = traderMessage.isEmpty() ? null : StringUtil.parseMessage(traderMessage);

        List<String> trades = config.get(type + ".trades", List.of());
        this.trades = NeoTradePool.parse(trades);

        this.rules = new HashMap<>();
        IConfig rules = config.getSection(type + ".rules");
        if (rules != null) rules.forEach(this.rules::put);
    }

    public TierSpecActions withMessage(String message, Object... args) {
        this.traderMessage = message;
        this.traderMessageArgs = args;
        return this;
    }

    public void apply(ServerLevel level) {
        for (Map.Entry<String, Object> rule : this.rules.entrySet())
            WorldRules.applyGameRule(level, rule.getKey(), rule.getValue());

        MinecraftServer server = level.getServer();
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        for (String command : this.commands)
            server.getCommands().performPrefixedCommand(source, command);

        WanderingTrader trader = NeoMerchant.spawnTraderWith(trades.getPool(), level, "UnlockTrader");
        if (trader != null)
            if (customTraderMessage != null) NeoBlock.sendInstantMessage(customTraderMessage, level, false);
            else NeoBlock.sendInstantMessage(traderMessage, level, false, traderMessageArgs);

        NeoBlock.sendInstantMessage(actionMessage, level, true);
        for (Component message: this.messages) NeoBlock.sendInstantMessage(message, level, false);
    }
}
