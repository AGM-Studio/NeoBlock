package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NeoArgumentInteger extends NeoArgument<Integer> {
    protected final int min;
    protected final int max;

    private static final SuggestionProvider<CommandSourceStack> dsp = (context, builder) -> {
        for (int i = 0; i <= 4; i++) builder.suggest(String.valueOf(Math.pow(10, i)));
        return builder.buildFuture();
    };

    private NeoArgumentInteger(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
        this.min = builder.min;
        this.max = builder.max;
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, IntegerArgumentType.integer(min, max)).suggests(provider);
    }

    @Override public Integer capture(CommandContext<CommandSourceStack> context, String key) {
        try {
            return IntegerArgumentType.getInteger(context, key);
        } catch (IllegalArgumentException e) {
            if (optional) return defaultValue;
            throw new RuntimeException("Unable to capture argument " + key, e);
        }
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private int defaultValue = 0;
        private SuggestionProvider<CommandSourceStack> provider = dsp;
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(int defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }
        public Builder provider(SuggestionProvider<CommandSourceStack> provider) { this.provider = provider; return this; }
        public Builder min(int min) { this.min = min; return this; }
        public Builder max(int max) { this.max = max; return this; }

        public NeoArgumentInteger build() {
            return new NeoArgumentInteger(this);
        }
    }
}