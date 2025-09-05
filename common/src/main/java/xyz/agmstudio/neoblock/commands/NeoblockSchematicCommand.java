package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentString;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.data.Schematic;

import java.nio.file.Path;

public class NeoblockSchematicCommand extends NeoCommand.ParentHolder {
    protected NeoblockSchematicCommand(NeoCommand parent) {
        super(parent, "scheme", 2);

        new Load(this);
        new Save(this);
    }

    public static class Load extends NeoCommand {
        protected Load(NeoCommand parent) {
            super(parent, "load");
            new NeoArgumentBlockPos.Builder(this, "pos").build();
            new NeoArgumentString.Builder(this, "name").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            CommandSourceStack source = context.getSource();
            ServerLevel level = context.getSource().getLevel();
            BlockPos origin = getArgument(context, "pos");
            String name = getArgument(context, "name");

            int result = Schematic.loadSchematic(level, origin, name);
            if (result == 0) return fail(context, "command.neoblock.scheme.load.not_found");
            if (result == -1) return fail(context, "command.neoblock.scheme.load.fail");
            
            return fail(context, "command.neoblock.scheme.load.success", origin.toShortString());
        }
    }

    public static class Save extends NeoCommand {
        protected Save(NeoCommand parent) {
            super(parent, "save", 2);
            new NeoArgumentBlockPos.Builder(this, "pos1").build();
            new NeoArgumentBlockPos.Builder(this, "pos2").build();
            new NeoArgumentBlockPos.Builder(this, "neoblock").defaultValue(null).build();
            new NeoArgumentString.Builder(this, "name").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            CommandSourceStack source = context.getSource();
            ServerLevel level = context.getSource().getLevel();
            BlockPos pos1 = getArgument(context, "pos1");
            BlockPos pos2 = getArgument(context, "pos2");
            BlockPos center = getArgument(context, "neoblock");
            String name = getArgument(context, "name");

            Path result = Schematic.saveSchematic(level, pos1, pos2, center, name);
            if (result == null) return fail(context, "command.neoblock.scheme.save.fail");
            return success(context, "command.neoblock.scheme.save.success", result.getFileName().toString());
        }
    }
}
