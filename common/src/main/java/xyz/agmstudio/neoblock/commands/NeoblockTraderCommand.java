package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.npc.WanderingTrader;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neocore.commands.NeoCommand;

public class NeoblockTraderCommand extends NeoCommand.ParentHolder {
    protected NeoblockTraderCommand(NeoCommand parent) {
        super(parent, "trader", 4);

        new Spawn(this);
    }

    public static class Spawn extends NeoCommand {
        protected Spawn(NeoCommand parent) {
            super(parent, "spawn");
            new NeoArgumentNeoBlock.Builder(this, "block").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlock block = this.getArgument(context, "block");
            WanderingTrader trader = NeoMerchant.forceSpawnTrader(block);
            if (trader != null)
                return success(context, "command.neoblock.force_trader.success");
            
            return fail(context, "command.neoblock.force_trader.failure");
        }
    }
}