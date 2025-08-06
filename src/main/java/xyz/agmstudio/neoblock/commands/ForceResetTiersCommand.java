package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.WorldData;

public class ForceResetTiersCommand extends NeoCommand {
    public ForceResetTiersCommand() {
        super("neoblock force reset", 4);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        WorldData.reloadTiers();
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.update.success"), true);
        return 1;
    }
}
