package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.function.Predicate;

public class NeoArgumentTier extends NeoArgument<TierSpec> {
    private static final DynamicCommandExceptionType EXCEPTION =
            new DynamicCommandExceptionType(size -> Component.translatable("command.neoblock.invalid_tier", size));

    public static SuggestionProvider<CommandSourceStack> createSuggester(final Predicate<TierSpec> filter) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            WorldData.getWorldTiers().stream().filter(filter).mapToInt(TierSpec::getID).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private NeoArgumentTier(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, IntegerArgumentType.integer(0)).suggests(provider);
    }

    @Override public TierSpec capture(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        int index = IntegerArgumentType.getInteger(context, key);
        if (index < 0 || index > WorldData.getWorldTiers().size())
            throw EXCEPTION.create(WorldData.getWorldTiers().size() - 1);

        return WorldData.getInstance().getTier(index);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private TierSpec defaultValue = null;
        private SuggestionProvider<CommandSourceStack> provider = createSuggester(s -> true);

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(TierSpec defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }
        public Builder provider(SuggestionProvider<CommandSourceStack> provider) { this.provider = provider; return this; }

        public NeoArgumentTier build() {
            return new NeoArgumentTier(this);
        }
    }
}