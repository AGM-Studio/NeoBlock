package xyz.agmstudio.neoblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.tiers.NeoTier;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@SuppressWarnings("SameReturnValue")
public class NeoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("neoblock").executes(NeoCommand::showInfo)
                        .then(Commands.literal("force")
                                .then(Commands.literal("trader").executes(NeoCommand::forceTrader))
                                .then(Commands.literal("update").executes(NeoCommand::forceUpdate)))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                        .suggests(NeoCommand::suggestTiersIndex).executes(NeoCommand::unlockTier)))
        );
    }

    // Methods that suggests arguments
    private static CompletableFuture<Suggestions> suggestTiersIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        IntStream.range(0, NeoBlock.TIERS.size()).forEach(builder::suggest);
        return builder.buildFuture();
    }

    // Methods that executes when the command is run
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getBlockCount(), WorldData.getUnlocked().stream().map(NeoTier::getName).toArray());

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
    private static int unlockTier(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int index = IntegerArgumentType.getInteger(context, "id");
        if (index < 0 || index > NeoBlock.TIERS.size()) {
            context.getSource().sendFailure(Component.translatable("command.neoblock.invalid_tier", NeoBlock.TIERS.size() - 1));
            return 0;
        }

        WorldData.setCommanded(index);
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.unlock_tier"), false);
        return 1;
    }
}
