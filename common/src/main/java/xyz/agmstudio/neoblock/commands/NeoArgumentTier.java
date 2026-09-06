package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neocore.commands.NeoArgument;
import xyz.agmstudio.neocore.commands.NeoCommand;

import java.util.function.Function;
import java.util.function.Predicate;

public class NeoArgumentTier extends NeoArgument<TierSpec> {
    private static final DynamicCommandExceptionType TIER_EXCEPTION =
            new DynamicCommandExceptionType(size -> Component.translatable("command.neoblock.invalid_tier", size));
    private static final SimpleCommandExceptionType BLOCK_EXCEPTION =
            new SimpleCommandExceptionType(Component.translatable("command.neoblock.invalid_block"));

    public static SuggestionProvider<CommandSourceStack> createSuggester(Function<CommandContext<CommandSourceStack>, NeoBlock> nbCapture, Predicate<TierSpec> filter) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            NeoBlock block = nbCapture.apply(context);
            if (block == null) return Suggestions.empty();
            block.getTierStream().filter(filter).map(TierSpec::getID).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private final Function<CommandContext<CommandSourceStack>, NeoBlock> nbCapture;
    private NeoArgumentTier(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
        this.nbCapture = builder.nbCapture;
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, StringArgumentType.string()).suggests(provider);
    }

    @Override public TierSpec capture(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        NeoBlock block = nbCapture.apply(context);
        if (block == null) throw BLOCK_EXCEPTION.create();

        String id = StringArgumentType.getString(context, key);
        TierSpec tier = block.getTier(id);
        if (tier == null) throw TIER_EXCEPTION.create(id);

        return tier;
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private TierSpec defaultValue = null;
        private SuggestionProvider<CommandSourceStack> provider;
        private final Function<CommandContext<CommandSourceStack>, NeoBlock> nbCapture;

        public Builder(NeoCommand base, String key, Function<CommandContext<CommandSourceStack>, NeoBlock> nbCapture) {
            this.nbCapture = nbCapture;
            this.base = base;
            this.key = key;

            this.provider = createSuggester(nbCapture, t -> true);
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