package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.commands.NeoArgument;
import xyz.agmstudio.neocore.commands.NeoCommand;

import java.util.function.Predicate;

public class NeoArgumentNeoBlock extends NeoArgument<NeoBlock> {
    public static final SimpleCommandExceptionType BLOCK_EXCEPTION =
            new SimpleCommandExceptionType(Component.translatable("command.neoblock.invalid_block"));

    private static final SuggestionProvider<CommandSourceStack> dsp = (context, builder) -> {
        WorldManager.getBlocks().stream().map(NeoBlock::getID).forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static SuggestionProvider<CommandSourceStack> createSuggester(Predicate<NeoBlock> filter) {
        return (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
            WorldManager.getBlocks().stream().filter(filter).map(NeoBlock::getID).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private NeoArgumentNeoBlock(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, builder.provider);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, StringArgumentType.string()).suggests(provider);
    }

    @Override public NeoBlock capture(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, key);
        NeoBlock block = WorldManager.getBlocks().stream().filter(b -> b.getID().equals(id)).findFirst().orElse(null);
        if (block == null) throw BLOCK_EXCEPTION.create();
        return block;
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private SuggestionProvider<CommandSourceStack> provider = dsp;
        private boolean optional = false;
        private NeoBlock defaultValue = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(NeoBlock defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }
        public Builder provider(SuggestionProvider<CommandSourceStack> provider) { this.provider = provider; return this; }

        public NeoArgumentNeoBlock build() {
            return new NeoArgumentNeoBlock(this);
        }
    }
}