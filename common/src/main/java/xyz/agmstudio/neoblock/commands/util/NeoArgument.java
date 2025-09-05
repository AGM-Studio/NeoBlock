package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;

public abstract class NeoArgument<T> {
    protected final String key;
    protected final boolean optional;
    protected final T defaultValue;
    protected final SuggestionProvider<CommandSourceStack> provider;

    public NeoArgument(NeoCommand base, String key, boolean optional, T defaultValue, SuggestionProvider<CommandSourceStack> suggestionProvider) {
        this.key = key;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.provider = suggestionProvider;

        if (!optional && !base.arguments.isEmpty()) {
            NeoArgument<?> last = base.arguments.values().stream().findFirst().get();
            if (last.isOptional()) throw new IllegalArgumentException("Required argument can not be defined after an optional argument");
        }
        base.arguments.put(key, this);
    }

    public boolean isOptional() {
        return optional;
    }
    public T getDefaultValue() {
        return defaultValue;
    }

    public T captureWithDefaultValue(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        try {
            return capture(context, key);
        } catch (IllegalArgumentException exception) {
            if (optional) return defaultValue;
            throw exception;
        }
    }

    public abstract ArgumentBuilder<CommandSourceStack, ?> build();
    public abstract T capture(CommandContext<CommandSourceStack> context, String key) throws IllegalArgumentException, CommandSyntaxException;
}
