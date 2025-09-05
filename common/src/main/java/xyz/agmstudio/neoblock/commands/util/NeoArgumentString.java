package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NeoArgumentString extends NeoArgument<String> {
    private NeoArgumentString(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, StringArgumentType.string()).suggests(provider);
    }

    @Override public String capture(CommandContext<CommandSourceStack> context, String key) {
        return StringArgumentType.getString(context, key);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private String defaultValue = null;
        private SuggestionProvider<CommandSourceStack> provider = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }
        public Builder provider(SuggestionProvider<CommandSourceStack> provider) { this.provider = provider; return this; }

        public NeoArgumentString build() {
            return new NeoArgumentString(this);
        }
    }
}