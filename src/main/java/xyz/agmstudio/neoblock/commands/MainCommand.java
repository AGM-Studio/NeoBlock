package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.UpgradeManager;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

@SuppressWarnings("SameReturnValue")
public class MainCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neoblock").executes(MainCommand::showInfo)
                        .then(Commands.literal("force")
                                .then(Commands.literal("trader").executes(MainCommand::forceTrader))
                                .then(Commands.literal("update").executes(MainCommand::forceUpdate))
                        )
        );
    }

    // Method that executes when the command is run
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getBlockCount())
                .append("\n  On upgrade:" + NeoBlock.isOnUpgrade())
                .append("\n  Animations:");

        for (Animation animation: UpgradeManager.getAllAnimations()) message.append("\n  - " + animation);

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
