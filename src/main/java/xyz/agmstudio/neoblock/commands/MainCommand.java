package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

public class MainCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neoblock").executes(MainCommand::showInfo)
                        .then(Commands.literal("force")
                                .then(Commands.literal("trader").executes(MainCommand::forceTrader)))
        );
    }

    // Method that executes when the command is run
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("command.neoblock.info", NeoBlock.DATA.getBlockCount(), NeoBlock.DATA.getTier().TIER)
                .append("\n  On upgrade:" + NeoBlock.UPGRADE.isOnUpgrade()), true);

        return 1;
    }
    private static int forceTrader(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WanderingTrader trader = NeoMerchant.forceSpawnTrader(source.getLevel());
        if (trader != null)
            source.sendSuccess(() -> Component.translatable("command.neoblock.force_trader.success"), true);
        else source.sendFailure(Component.translatable("command.neoblock.force_trader.failure"));
        return 1;
    }
}
