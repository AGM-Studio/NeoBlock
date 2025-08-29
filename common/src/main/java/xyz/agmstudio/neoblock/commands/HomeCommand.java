package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;

public class HomeCommand extends NeoCommand {
    public HomeCommand() {
        super("neoblock home", 0);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() == null) {
            source.sendFailure(Component.translatable("command.neoblock.home.not_entity"));
            return 0;
        }

        BlockManager.getSafeBlock().teleportTo(source.getEntity());
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.home", source.getEntity().getDisplayName()), true);
        return 1;
    }
}
