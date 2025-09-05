package xyz.agmstudio.neoblock.commands.util;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;

public class NeoArgumentBlockPos extends NeoArgument<BlockPos> {
    private NeoArgumentBlockPos(Builder builder) {
        super(builder.base, builder.key, builder.optional, builder.defaultValue, null);
    }

    @Override public ArgumentBuilder<CommandSourceStack, ?> build() {
        return Commands.argument(key, BlockPosArgument.blockPos());
    }

    @Override public BlockPos capture(CommandContext<CommandSourceStack> context, String key) {
        return BlockPosArgument.getBlockPos(context, key);
    }

    public static class Builder {
        private final NeoCommand base;
        private final String key;
        private boolean optional = false;
        private BlockPos defaultValue = null;

        public Builder(NeoCommand base, String key) {
            this.base = base;
            this.key = key;
        }

        public Builder defaultValue(BlockPos defaultValue) {
            this.defaultValue = defaultValue;
            this.optional = true;
            return this;
        }

        public NeoArgumentBlockPos build() {
            return new NeoArgumentBlockPos(this);
        }
    }
}