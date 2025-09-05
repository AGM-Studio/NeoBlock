package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NeoArgumentBoolean extends NeoArgument<Boolean> {
    private NeoArgumentBoolean(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, BoolArgumentType.bool());
    }

    @Override public Boolean capture(CommandContext<CommandSourceStack> context, String key) {
        return BoolArgumentType.getBool(context, key);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private boolean defaultValue = false;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public NeoArgumentBoolean build() {
            return new NeoArgumentBoolean(this);
        }
    }
}