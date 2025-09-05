package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentDimension;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.Optional;

public class NeoblockForceCommand extends NeoCommand.ParentHolder {
    protected NeoblockForceCommand(NeoCommand parent) {
        super(parent, "force", 4);

        new SetBlock(this);
        new Stop(this);
        new TraderSpawn(this);
        new ResetTiers(this);
    }

    public static class SetBlock extends NeoCommand {
        protected SetBlock(NeoCommand parent) {
            super(parent, "setblock");
            new NeoArgumentBlockPos.Builder(this, "pos").build();
            new NeoArgumentDimension.Builder(this, "dimension").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            BlockPos origin = getArgument(context, "pos");
            ServerLevel world = getArgument(context, "dimension", context.getSource()::getLevel);

            WorldData.getWorldStatus().setBlockPos(origin, WorldData.getWorldLevel());
            WorldData.getWorldStatus().setDimension(world);
            String message = "command.neoblock.force_block";
            if (WorldData.getWorldStatus().isDisabled()) {
                WorldData.getWorldStatus().setActive();
                message += ".enabled";
            }
            
            BlockManager.updateBlock(WorldData.getWorldLevel(), false);
            return success(context, message);
        }
    }

    public static class Stop extends NeoCommand {
        protected Stop(NeoCommand parent) {
            super(parent, "stop");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            BlockManager.cleanBlock(WorldData.getWorldLevel(), NeoBlockPos.get());
            WorldData.getWorldStatus().setDisabled();

            Optional<SetBlock> command = NeoCommand.getFromRegistry(SetBlock.class);
            return success(context, "command.neoblock.disabled", command.map(NeoCommand::getCommand).orElse(null));
        }
    }

    public static class TraderSpawn extends NeoCommand {
        protected TraderSpawn(NeoCommand parent) {
            super(parent, "trader");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            CommandSourceStack source = context.getSource();
            WanderingTrader trader = NeoMerchant.forceSpawnTrader(source.getLevel());
            if (trader != null)
                return success(context, "command.neoblock.force_trader.success");
            
            return fail(context, "command.neoblock.force_trader.failure");
        }
    }

    public static class ResetTiers extends NeoCommand {
        protected ResetTiers(NeoCommand parent) {
            super(parent, "reset");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            WorldData.resetTiers();
            return success(context, "command.neoblock.update.success");
        }
    }
}
