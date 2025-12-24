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
    private final List<String> commands;
    private final List<Component> messages;
    private final NeoTradePool trades;
    private final HashMap<String, Object> rules;

    private String traderMessage = null;
    private Object[] traderMessageArgs = new Object[] {};

    public TierSpecActions(@NotNull IConfig config, String type) {
        this.commands = config.get(type + ".commands", List.of());
        List<String> messages = config.get(type + ".messages", List.of());
        this.messages = messages.stream().map(StringUtil::parseMessage).collect(Collectors.toList());

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
        if (trader != null) NeoBlock.sendInstantMessage(traderMessage, level, false, traderMessageArgs);

        for (Component message: this.messages) NeoBlock.sendInstantMessage(message, level, false);
    }
}
