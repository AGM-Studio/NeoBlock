package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

public class NeoArgumentEntityType extends NeoArgument<EntityType<?>> {
    private final CommandBuildContext buildContext;

    private NeoArgumentEntityType(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
        this.buildContext = builder.buildContext;
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE));
    }

    @Override
    public EntityType<?> capture(CommandContext<CommandSourceStack> context, String key) throws NeoCommand.CommandExtermination {
        try {
            return ResourceArgument.getEntityType(context, key).value();
        } catch (IllegalArgumentException | CommandSyntaxException e) {
            if (optional) return defaultValue;
            throw new RuntimeException("Unable to capture entity type " + key, e);
        }
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private EntityType<?> defaultValue = null;
        public CommandBuildContext buildContext = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(EntityType<?> defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public NeoArgumentEntityType build(CommandBuildContext context) {
            this.buildContext = context;
            return new NeoArgumentEntityType(this);
        }
    }
}
