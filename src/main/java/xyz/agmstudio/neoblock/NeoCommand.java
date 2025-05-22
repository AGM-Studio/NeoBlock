package xyz.agmstudio.neoblock;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.merchants.NeoMerchant;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("SameReturnValue")
public class NeoCommand {
    private static BlockPos getBlockPos(CommandContext<CommandSourceStack> context, String name) {
        try {
            return BlockPosArgument.getBlockPos(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
    private static String getString(CommandContext<CommandSourceStack> context, String name) {
        try {
            return StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
    private static boolean getBool(CommandContext<CommandSourceStack> context, String name) {
        return getBool(context, name, false);
    }
    private static boolean getBool(CommandContext<CommandSourceStack> context, String name, boolean defaultValue) {
        try {
            return BoolArgumentType.getBool(context, name);
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }
    private static WorldTier getTier(CommandContext<CommandSourceStack> context, String name) {
        int index = IntegerArgumentType.getInteger(context, "id");
        if (index < 0 || index > WorldData.getTiers().size()) {
            context.getSource().sendFailure(Component.translatable("command.neoblock.invalid_tier", WorldData.getTiers().size() - 1));
            return null;
        }

        return WorldData.getInstance().getTier(index);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> COMMAND = Commands.literal("neoblock").executes(NeoCommand::showInfo);

        final LiteralArgumentBuilder<CommandSourceStack> FORCE_COMMAND = Commands.literal("force");
        FORCE_COMMAND.then(Commands.literal("trader").executes(NeoCommand::forceTrader));
        FORCE_COMMAND.then(Commands.literal("update").executes(NeoCommand::forceUpdate));
        COMMAND.then(FORCE_COMMAND);

        final LiteralArgumentBuilder<CommandSourceStack> DISABLE = Commands.literal("disable")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .suggests(NeoCommand::suggestEnabledTiersIndex)).executes(NeoCommand::disableTier);
        COMMAND.then(DISABLE);

        final LiteralArgumentBuilder<CommandSourceStack> ENABLE = Commands.literal("enable")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .suggests(NeoCommand::suggestDisabledTiersIndex)).executes(NeoCommand::enableTier);
        COMMAND.then(ENABLE);

        final LiteralArgumentBuilder<CommandSourceStack> UNLOCK = Commands.literal("unlock")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .suggests(NeoCommand::suggestLockedTiersIndex).executes(NeoCommand::unlockTier)
                        .then(Commands.argument("force", BoolArgumentType.bool()).executes(NeoCommand::unlockTier)));
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
    private static CompletableFuture<Suggestions> suggestLockedTiersIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        WorldData.getTiers().stream().filter(tier -> !tier.isUnlocked()).mapToInt(WorldTier::getID).forEach(builder::suggest);
        return builder.buildFuture();
    }
    private static CompletableFuture<Suggestions> suggestDisabledTiersIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        WorldData.getTiers().stream().filter(tier -> !tier.isEnabled()).mapToInt(WorldTier::getID).forEach(builder::suggest);
        return builder.buildFuture();
    }
    private static CompletableFuture<Suggestions> suggestEnabledTiersIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        WorldData.getTiers().stream().filter(WorldTier::isEnabled).mapToInt(WorldTier::getID).forEach(builder::suggest);
        return builder.buildFuture();
    }

    // Methods that executes when the command is run
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getBlockCount(), WorldData.getTiers().stream().filter(tier -> !tier.isUnlocked()).map(WorldTier::getName).toArray());

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
        WorldTier tier = getTier(context, "id");
        boolean force = getBool(context, "force", true);

        if (tier == null) return 0;
        WorldData.setCommanded(tier, force);
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.unlock_tier"), false);
        return 1;
    }
    private static int disableTier(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WorldTier tier = getTier(context, "id");
        if (tier == null) return 0;
        else tier.disable();

        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.disable_tier"), false);
        return 1;
    }
    private static int enableTier(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WorldTier tier = getTier(context, "id");
        if (tier == null) return 0;
        else tier.enable();

        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.enable_tier"), false);
        return 1;
    }

    private static int schemeSave(CommandContext<CommandSourceStack> context)  {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos1 = getBlockPos(context, "pos1");
        BlockPos pos2 = getBlockPos(context, "pos2");
        BlockPos center = getBlockPos(context, "neoblock");
        String name = getString(context, "name");

        assert pos1 != null && pos2 != null;    // required

        Path result = Schematic.saveSchematic(level, pos1, pos2, center, name);
        if (result == null) source.sendFailure(Component.translatable("command.neoblock.scheme.save.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.save.success", result.getFileName().toString()), true);
        return result != null ? 1 : 0;
    }
    private static int schemeLoad(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos origin = getBlockPos(context, "pos");
        String name = getString(context, "name");

        assert origin != null;

        int result = Schematic.loadSchematic(level, origin, name);
        if (result == 0) source.sendFailure(Component.translatable("command.neoblock.scheme.load.not_found"));
        else if (result == -1) source.sendFailure(Component.translatable("command.neoblock.scheme.load.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.load.success", origin.toShortString()), true);
        return result == 1 ? 1 : 0;
    }
}
