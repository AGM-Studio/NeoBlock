package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;

public class NeoArgumentDimension extends NeoArgument<ServerLevel> {
    private NeoArgumentDimension(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, DimensionArgument.dimension());
    }

    @Override public ServerLevel capture(CommandContext<CommandSourceStack> context, String key) {
        try {
            return DimensionArgument.getDimension(context, key);
        } catch (IllegalArgumentException | CommandSyntaxException e) {
            if (optional) return defaultValue;
            throw new RuntimeException("Unable to capture argument " + key, e);
        }
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private ServerLevel defaultValue = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(ServerLevel defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public NeoArgumentDimension build() {
            return new NeoArgumentDimension(this);
        }
    }
}