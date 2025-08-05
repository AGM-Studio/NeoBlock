package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

import java.util.function.Predicate;

public class NeoArgumentTier extends NeoArgument<WorldTier> {
    public static SuggestionProvider<CommandSourceStack> createSuggester(final Predicate<WorldTier> filter) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            WorldData.getWorldTiers().stream().filter(filter).mapToInt(WorldTier::getID).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private NeoArgumentTier(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, IntegerArgumentType.integer(0, TierData.size() - 1)).suggests(provider);
    }

    @Override public WorldTier capture(CommandContext<CommandSourceStack> context, String key) throws NeoCommand.CommandExtermination {
        try {
            int index = IntegerArgumentType.getInteger(context, key);
            if (index < 0 || index > WorldData.getWorldTiers().size()) {
                context.getSource().sendFailure(Component.translatable("command.neoblock.invalid_tier", WorldData.getWorldTiers().size() - 1));
                throw new NeoCommand.CommandExtermination();
            }
            return WorldData.getInstance().getTier(index);
        } catch (IllegalArgumentException e) {
            if (optional) return defaultValue;
            throw new RuntimeException("Unable to capture argument " + key, e);
        }
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private WorldTier defaultValue = null;
        private SuggestionProvider<CommandSourceStack> provider = createSuggester(s -> true);

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(WorldTier defaultValue) {
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