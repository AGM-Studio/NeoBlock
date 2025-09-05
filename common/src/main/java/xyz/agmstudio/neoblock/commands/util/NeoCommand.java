package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.util.JavaUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;


@SuppressWarnings("unchecked")
public abstract class NeoCommand {
    private static final List<NeoCommand> registry = new ArrayList<>();
    private static final DynamicCommandExceptionType UNEXPECTED_EXCEPTION =
            new DynamicCommandExceptionType(e -> Component.translatable("command.neoblock.unexpected_exception", ((Exception) e).getMessage()));

    public static <T extends NeoCommand> Optional<T> getFromRegistry(Class<T> clazz) {
        for (NeoCommand command : registry)
            if (command.getClass() == clazz)
                return Optional.of((T) command);

        return Optional.empty();
    }

    protected final LinkedHashMap<String, NeoArgument<?>> arguments = new LinkedHashMap<>();
    protected final CommandBuildContext buildContext;
    protected final Predicate<CommandSourceStack> permission;
    protected final int permission_value;
    protected final String pattern;

    private final List<NeoCommand> subcommands = new ArrayList<>();

    public NeoCommand(CommandBuildContext buildContext, String pattern) {
        this(buildContext, pattern, 0);
    }
    public NeoCommand(CommandBuildContext buildContext, String pattern, int permission) {
        this.buildContext = buildContext;
        this.permission_value = permission;
        this.permission = context -> context.hasPermission(permission);
        this.pattern = pattern;
        registry.add(this);
    }

    public NeoCommand(NeoCommand parent, String pattern) {
        this(parent.buildContext, "%s %s".formatted(parent.pattern, pattern), Math.max(0, parent.permission_value));
        parent.subcommands.add(this);
    }
    public NeoCommand(NeoCommand parent, String pattern, int permission) {
        this(parent.buildContext, "%s %s".formatted(parent.pattern, pattern), Math.max(permission, parent.permission_value));
        parent.subcommands.add(this);
    }

    public MutableComponent getCommand() {
        return Component.literal("/" + this.pattern).withStyle(
                Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + this.pattern)).withColor(ChatFormatting.AQUA)
        );
    }

    public <T> T getArgument(CommandContext<CommandSourceStack> context, String key, Class<T> type) throws CommandSyntaxException {
        NeoArgument<T> argument = (NeoArgument<T>) arguments.get(key);
        if (argument == null) throw new IllegalArgumentException("No such argument has been defined for this command: " + key);
        return argument.captureWithDefaultValue(context, key);
    }
    public <T> T getArgument(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        NeoArgument<T> argument = (NeoArgument<T>) arguments.get(key);
        if (argument == null) throw new IllegalArgumentException("No such argument has been defined for this command: " + key);
        return argument.captureWithDefaultValue(context, key);
    }
    public <T> T getArgument(CommandContext<CommandSourceStack> context, String key, Supplier<T> defaultValue) throws CommandSyntaxException {
        NeoArgument<T> argument = (NeoArgument<T>) arguments.get(key);
        if (argument == null) throw new IllegalArgumentException("No such argument has been defined for this command: " + key);
        T value = argument.captureWithDefaultValue(context, key);
        return value != null ? value : defaultValue.get();
    }
    public abstract int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

    public List<LiteralCommandNode<CommandSourceStack>> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Command<CommandSourceStack> executor = (context) -> {
            try {
                return this.execute(context);
            } catch (CommandSyntaxException e) {
                throw e;
            } catch (Exception e) {
                NeoBlock.LOGGER.error("Unhandled exception while executing command", e);
                throw UNEXPECTED_EXCEPTION.create(e);
            }
        };

        List<String> parts = new ArrayList<>(List.of(pattern.split(" ")));
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(parts.remove(0));

        ArgumentBuilder<CommandSourceStack, ?> current = null;
        NeoArgument<?> previous = null;
        for (NeoArgument<?> argument: JavaUtil.reverse(arguments.values())) {
            ArgumentBuilder<CommandSourceStack, ?> build = argument.build().requires(permission);
            if (previous == null || previous.isOptional()) build.executes(executor);
            if (current != null) build.then(current);
            previous = argument;
            current = build;
        }

        while (!parts.isEmpty()) {
            ArgumentBuilder<CommandSourceStack, ?> build = Commands.literal(parts.remove(parts.size() - 1)).requires(permission);
            if (current == null) build.executes(executor);
            else build.then(current);
            current = build;
        }

        if (current == null) root.executes(executor);
        else root.then(current);

        List<LiteralCommandNode<CommandSourceStack>> commands = new ArrayList<>();
        commands.add(dispatcher.register(root));
        for (NeoCommand subcommand: subcommands) commands.addAll(subcommand.register(dispatcher));
        return commands;
    }

    protected int success(@NotNull CommandContext<CommandSourceStack> context, @NotNull Supplier<Component> message) {
        context.getSource().sendSuccess(message, true);
        return 1;
    }
    protected int success(@NotNull CommandContext<CommandSourceStack> context, @NotNull Component message) {
        return success(context, () -> message);
    }
    protected int success(@NotNull CommandContext<CommandSourceStack> context, @NotNull String message, Object ... args) {
        return success(context, () -> Component.translatable(message, args));
    }

    protected int fail(@NotNull CommandContext<CommandSourceStack> context, @NotNull Supplier<Component> message) {
        context.getSource().sendFailure(message.get());
        return 0;
    }
    protected int fail(@NotNull CommandContext<CommandSourceStack> context, @NotNull Component message) {
        context.getSource().sendFailure(message);
        return 0;
    }
    protected int fail(@NotNull CommandContext<CommandSourceStack> context, @NotNull String message, Object ... args) {
        return fail(context, Component.translatable(message, args));
    }

    public static class ParentHolder extends NeoCommand {
        protected ParentHolder(NeoCommand parent, String pattern, int permission) {
            super(parent, pattern, permission);
        }
        protected ParentHolder(NeoCommand parent, String pattern) {
            super(parent, pattern, parent.permission_value);
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
        }
    }
}
