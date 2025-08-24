package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentString;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.data.Schematic;

public class SchematicLoadCommand extends NeoCommand {
    public SchematicLoadCommand() {
        super("neoblock scheme load", 2);
        new NeoArgumentBlockPos.Builder(this, "pos").build();
        new NeoArgumentString.Builder(this, "name").defaultValue(null).build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos origin = getArgument(context, "pos");
        String name = getArgument(context, "name");

        int result = Schematic.loadSchematic(level, origin, name);
        if (result == 0) source.sendFailure(Component.translatable("command.neoblock.scheme.load.not_found"));
        else if (result == -1) source.sendFailure(Component.translatable("command.neoblock.scheme.load.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.load.success", origin.toShortString()), true);
        return result == 1 ? 1 : 0;
    }
}
