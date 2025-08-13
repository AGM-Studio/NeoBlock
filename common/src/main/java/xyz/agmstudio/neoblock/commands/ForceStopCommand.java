package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.Optional;

public class ForceStopCommand extends NeoCommand {
    public ForceStopCommand() {
        super("neoblock force stop", 4);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        BlockManager.cleanBlock(WorldData.getWorldLevel(), BlockManager.getBlockPos());
        WorldData.getWorldStatus().setDisabled();

        Optional<ForceBlockCommand> command = NeoCommand.getFromRegistry(ForceBlockCommand.class);
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.disabled", command.map(NeoCommand::getCommand).orElse(null)), true);
        return 1;
    }
}
