package xyz.agmstudio.neoblock.neo.events;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoTradePool;
import xyz.agmstudio.neoblock.neo.world.WorldCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldRules;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class NeoEventAction {
    private final int cooldown;
    private final List<String> commands;
    private final List<Component> messages;
    private final Component actionMessage;
    private final Component customTraderMessage;
    private final NeoTradePool trades;
    private final HashMap<String, Object> rules;

    private String traderMessage = null;
    private Object[] traderMessageArgs = new Object[] {};

    public NeoEventAction(@NotNull IConfig config, String type) {
        this.cooldown = config.getInt(type + ".cooldown", 0);
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

    public NeoEventAction withMessage(String message, Object... args) {
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
        if (cooldown > 0) WorldCooldown.Type.Normal.create(cooldown);
    }
}
