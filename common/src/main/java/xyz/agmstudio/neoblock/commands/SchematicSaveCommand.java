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

import java.nio.file.Path;

public class SchematicSaveCommand extends NeoCommand {
    public SchematicSaveCommand() {
        super("neoblock scheme save", 2);
        new NeoArgumentBlockPos.Builder(this, "pos1").build();
        new NeoArgumentBlockPos.Builder(this, "pos2").build();
        new NeoArgumentBlockPos.Builder(this, "neoblock").defaultValue(null).build();
        new NeoArgumentString.Builder(this, "name").defaultValue(null).build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos1 = getArgument(context, "pos1");
        BlockPos pos2 = getArgument(context, "pos2");
        BlockPos center = getArgument(context, "neoblock");
        String name = getArgument(context, "name");

        Path result = Schematic.saveSchematic(level, pos1, pos2, center, name);
        if (result == null) source.sendFailure(Component.translatable("command.neoblock.scheme.save.fail"));
        else source.sendSuccess(() -> Component.translatable("command.neoblock.scheme.save.success", result.getFileName().toString()), true);
        return result != null ? 1 : 0;
    }
}
