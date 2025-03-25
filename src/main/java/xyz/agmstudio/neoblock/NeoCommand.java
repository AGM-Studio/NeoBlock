package xyz.agmstudio.neoblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

public class NeoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neoblock").executes(NeoCommand::showInfo)
                        .then(Commands.literal("force")
                                .then(Commands.literal("trader").executes(NeoCommand::forceTrader))
                                .then(Commands.literal("update").executes(NeoCommand::forceUpdate))
                        )
        );
    }

    // Method that executes when the command is run
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getBlockCount());

        source.sendSuccess(() -> message, true);
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
    private static int forceUpdate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WorldData.updateTiers();
        source.sendSuccess(() -> Component.translatable("command.neoblock.update.success"), true);
        return 1;
    }
}
