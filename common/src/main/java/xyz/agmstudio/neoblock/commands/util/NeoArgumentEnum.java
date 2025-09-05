package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NeoArgumentEnum<T extends Enum<T>> extends NeoArgument<T> {
    private final Class<T> clazz;

    private NeoArgumentEnum(Builder<T> builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
        this.clazz = builder.clazz;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build() {
        SuggestionProvider<CommandSourceStack> suggestions = (context, builder) -> {
            for (T constant : clazz.getEnumConstants()) builder.suggest(constant.name().toLowerCase());
            return builder.buildFuture();
        };

        return Commands.argument(key, StringArgumentType.word()).suggests(provider != null ? provider : suggestions);
    }

    @Override
    public T capture(CommandContext<CommandSourceStack> context, String key) {
        String input = StringArgumentType.getString(context, key);

        for (T constant : clazz.getEnumConstants())
            if (constant.name().equalsIgnoreCase(input)) return constant;

        throw new IllegalArgumentException("Invalid enum value: " + input);
    }

    // Builder for NeoArgumentEnum
    public static class Builder<T extends Enum<T>> {
        private final NeoCommand base;
        private final String key;
        private final Class<T> clazz;
        private boolean optional = false;
        private T defaultValue = null;
        private SuggestionProvider<CommandSourceStack> provider = null;

        public Builder(NeoCommand base, String key, Class<T> clazz) {
            this.base = base;
            this.key = key;
            this.clazz = clazz;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public Builder<T> provider(SuggestionProvider<CommandSourceStack> provider) {
            this.provider = provider;
            return this;
        }

        public NeoArgumentEnum<T> build() {
            return new NeoArgumentEnum<>(this);
        }
    }
}
