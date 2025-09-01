package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentDimension;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.Optional;

public class NeoblockForceCommand extends NeoCommand {
    public NeoblockForceCommand(NeoCommand parent) {
        super(parent, "force", 4);

        new SetBlock(this);
        new Stop(this);
        new TraderSpawn(this);
        new ResetTiers(this);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.force"), false);
        return 1;
    }

    public static class SetBlock extends NeoCommand {
        public SetBlock(NeoCommand parent) {
            super(parent, "setblock");
            new NeoArgumentBlockPos.Builder(this, "pos").build();
            new NeoArgumentDimension.Builder(this, "dimension").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            CommandSourceStack source = context.getSource();
            BlockPos origin = getArgument(context, "pos");
            ServerLevel world = getArgument(context, "dimension", source::getLevel);

            WorldData.getWorldStatus().setBlockPos(origin, WorldData.getWorldLevel());
            WorldData.getWorldStatus().setDimension(world);
            if (WorldData.getWorldStatus().isDisabled()) {
                WorldData.getWorldStatus().setActive();
                source.sendSuccess(() -> Component.translatable("command.neoblock.force_block.enabled"), true);
            } else source.sendSuccess(() -> Component.translatable("command.neoblock.force_block"), true);
            BlockManager.updateBlock(WorldData.getWorldLevel(), false);

            return 1;
        }
    }

    public static class Stop extends NeoCommand {
        public Stop(NeoCommand parent) {
            super(parent, "stop");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            BlockManager.cleanBlock(WorldData.getWorldLevel(), BlockManager.getBlockPos());
            WorldData.getWorldStatus().setDisabled();

            Optional<SetBlock> command = NeoCommand.getFromRegistry(SetBlock.class);
            context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.disabled", command.map(NeoCommand::getCommand).orElse(null)), true);
            return 1;
        }
    }

    public static class TraderSpawn extends NeoCommand {
        public TraderSpawn(NeoCommand parent) {
            super(parent, "trader");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            CommandSourceStack source = context.getSource();
            WanderingTrader trader = NeoMerchant.forceSpawnTrader(source.getLevel());
            if (trader != null)
                source.sendSuccess(() -> Component.translatable("command.neoblock.force_trader.success"), true);
            else source.sendFailure(Component.translatable("command.neoblock.force_trader.failure"));
            return 1;
        }
    }

    public static class ResetTiers extends NeoCommand {
        public ResetTiers(NeoCommand parent) {
            super(parent, "reset");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            WorldData.resetTiers();
            context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.update.success"), true);
            return 1;
        }
    }
}
