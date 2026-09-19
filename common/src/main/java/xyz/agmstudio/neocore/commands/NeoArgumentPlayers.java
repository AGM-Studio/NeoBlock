package xyz.agmstudio.neocore.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class NeoArgumentPlayers extends NeoArgument<Collection<ServerPlayer>> {
    private NeoArgumentPlayers(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, EntityArgument.players());
    }

    @Override
    public Collection<ServerPlayer> capture(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        return EntityArgument.getPlayers(context, key);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private Collection<ServerPlayer> defaultValue = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(Collection<ServerPlayer> defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public NeoArgumentPlayers build() {
            return new NeoArgumentPlayers(this);
        }
    }
}