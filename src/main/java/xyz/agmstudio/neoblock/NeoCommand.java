package xyz.agmstudio.neoblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import xyz.agmstudio.neoblock.data.NeoSchematic;
import xyz.agmstudio.neoblock.tiers.NeoTier;
import xyz.agmstudio.neoblock.tiers.TierManager;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoMerchant;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@SuppressWarnings("SameReturnValue")
public class NeoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> COMMAND = Commands.literal("neoblock").executes(NeoCommand::showInfo);

        final LiteralArgumentBuilder<CommandSourceStack> FORCE_COMMAND = Commands.literal("force");
        FORCE_COMMAND.then(Commands.literal("trader").executes(NeoCommand::forceTrader));
        FORCE_COMMAND.then(Commands.literal("update").executes(NeoCommand::forceUpdate));
        COMMAND.then(FORCE_COMMAND);

        final LiteralArgumentBuilder<CommandSourceStack> UNLOCK = Commands.literal("unlock")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .suggests(NeoCommand::suggestTiersIndex).executes(NeoCommand::unlockTier));
        COMMAND.then(UNLOCK);

        final LiteralArgumentBuilder<CommandSourceStack> SCHEME = Commands.literal("scheme");
        final LiteralArgumentBuilder<CommandSourceStack> SCHEME_SAVE = Commands.literal("save")
                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                        .then(Commands.argument("pos2", BlockPosArgument.blockPos()).executes(NeoCommand::schemeSave)
                                .then(Commands.argument("neoblock", BlockPosArgument.blockPos()).executes(NeoCommand::schemeSave)
                                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(NeoCommand::schemeSave)))));
        SCHEME.then(SCHEME_SAVE);

        final LiteralArgumentBuilder<CommandSourceStack> SCHEME_LOAD = Commands.literal("load")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(NeoCommand::schemeLoad)
                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(NeoCommand::schemeLoad)));
        SCHEME.then(SCHEME_LOAD);
        COMMAND.then(SCHEME);

        dispatcher.register(COMMAND);
    }

    // Methods that suggests arguments
    private static CompletableFuture<Suggestions> suggestTiersIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        IntStream.range(0, TierManager.TIERS.size()).forEach(builder::suggest);
        return builder.buildFuture();
    }
    public static final SuggestionProvider<CommandSourceStack> LOOKING_BLOCK_SUGGESTION = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return Suggestions.empty();

        HitResult hit = player.pick(20, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return Suggestions.empty();
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        String suggestion = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return SharedSuggestionProvider.suggest(Collections.singletonList(suggestion), builder);
    };

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
        if (index < 0 || index > TierManager.TIERS.size()) {
            context.getSource().sendFailure(Component.translatable("command.neoblock.invalid_tier", TierManager.TIERS.size() - 1));
            return 0;
        }

        WorldData.setCommanded(index);
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.unlock_tier"), false);
        return 1;
    }

    private static int schemeSave(CommandContext<CommandSourceStack> context)  {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");
        BlockPos center = null;
        try {
            center = BlockPosArgument.getBlockPos(context, "neoblock");
        } catch (IllegalArgumentException ignored) {}

        String name = null;
        try {
            name = StringArgumentType.getString(context, "name");
        } catch (IllegalArgumentException ignored) {}

        Path result = NeoSchematic.saveSchematic(level, pos1, pos2, center, name);
        if (result == null) source.sendFailure(Component.translatable("command.neoblock.scheme.save.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.save.success", result.getFileName().toString()), true);
        return result != null ? 1 : 0;
    }
    private static int schemeLoad(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos origin = BlockPosArgument.getBlockPos(context, "pos");

        String name = null;
        try {
            name = StringArgumentType.getString(context, "name");
        } catch (IllegalArgumentException ignored) {}

        int result = NeoSchematic.loadSchematic(level, origin, name);
        if (result == 0) source.sendFailure(Component.translatable("command.neoblock.scheme.load.not_found"));
        else if (result == -1) source.sendFailure(Component.translatable("command.neoblock.scheme.load.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.load.success", origin.toShortString()), true);
        return result == 1 ? 1 : 0;
    }
}
