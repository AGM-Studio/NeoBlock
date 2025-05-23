package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.merchants.NeoMerchant;

public class ForceTraderSpawnCommand extends NeoCommand {
    public ForceTraderSpawnCommand() {
        super("neoblock force trader", 1);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        WanderingTrader trader = NeoMerchant.forceSpawnTrader(source.getLevel());
        if (trader != null)
            source.sendSuccess(() -> Component.translatable("command.neoblock.force_trader.success"), true);
        else source.sendFailure(Component.translatable("command.neoblock.force_trader.failure"));
        return 1;
    }
}
