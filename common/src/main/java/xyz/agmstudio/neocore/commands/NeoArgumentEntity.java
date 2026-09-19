package xyz.agmstudio.neocore.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;

public class NeoArgumentEntity extends NeoArgument<Entity> {
    private NeoArgumentEntity(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, EntityArgument.entity());
    }

    @Override
    public Entity capture(CommandContext<CommandSourceStack> context, String key) throws CommandSyntaxException {
        return EntityArgument.getEntity(context, key);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private Entity defaultValue = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(Entity defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }
        public Builder single(boolean single) {
            return this;
        }

        public NeoArgumentEntity build() {
            return new NeoArgumentEntity(this);
        }
    }
}