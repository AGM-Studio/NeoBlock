package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.neo.tiers.TierConfig;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.commands.NeoArgumentBlockPos;
import xyz.agmstudio.neocore.commands.NeoArgumentDimension;
import xyz.agmstudio.neocore.commands.NeoArgumentString;
import xyz.agmstudio.neocore.commands.NeoCommand;

import java.util.HashMap;
import java.util.Optional;

public class NeoblockForceCommand extends NeoCommand.ParentHolder {
    protected NeoblockForceCommand(NeoCommand parent) {
        super(parent, "force", 4);

        new SetBlockPos(this);
        new SetBlockGroup(this);
        new AddBlock(this);
        new RemoveBlock(this);

        new Stop(this);
        new Activate(this);
    }

    public static class SetBlockPos extends NeoCommand {
        protected SetBlockPos(NeoCommand parent) {
            super(parent, "block setpos");
            new NeoArgumentNeoBlock.Builder(this, "block").build();
            new NeoArgumentBlockPos.Builder(this, "pos").build();
            new NeoArgumentDimension.Builder(this, "dimension").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlock block = this.getArgument(context, "block");
            BlockPos origin = this.getArgument(context, "pos");
            ServerLevel world = this.getArgument(context, "dimension", context.getSource()::getLevel);

            block.setDimension(world.dimension());
            block.setBlockPos(origin, world);
            if (block.isBedrock()) block.updateBlock(false);
            return success(context, "command.neoblock.set_block_pos");
        }
    }

    public static class SetBlockGroup extends NeoCommand {
        protected SetBlockGroup(NeoCommand parent) {
            super(parent, "block setgroup");
            new NeoArgumentNeoBlock.Builder(this, "block").build();
            new NeoArgumentString.Builder(this, "group").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            String group = this.getArgument(context, "group");
            HashMap<String, TierConfig> cfg = WorldManager.getTierConfigGroup(group);
            if (cfg == null) return fail(context, "command.neoblock.invalid_tier_group");

            NeoBlock block = this.getArgument(context, "block");
            block.setGroup(group);
            block.initiate();

            return success(context, "command.neoblock.set_block_group", group);
        }
    }

    public static class AddBlock extends NeoCommand {
        protected AddBlock(NeoCommand parent) {
            super(parent, "block add");
            new NeoArgumentString.Builder(this, "id").build();
            new NeoArgumentBlockPos.Builder(this, "pos").build();
            new NeoArgumentDimension.Builder(this, "dimension").defaultValue(null).build();
            new NeoArgumentString.Builder(this, "group").defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            String id = this.getArgument(context, "id");
            BlockPos origin = this.getArgument(context, "pos");
            ServerLevel world = this.getArgument(context, "dimension", context.getSource()::getLevel);
            String group = this.getArgument(context, "group");

            NeoBlock generated = NeoBlock.create(world, id, origin, group);
            generated.initiate();
            WorldManager.addNeoBlock(generated);
            return success(context, "command.neoblock.add_block");
        }
    }

    public static class RemoveBlock extends NeoCommand {
        protected RemoveBlock(NeoCommand parent) {
            super(parent, "block remove");
            new NeoArgumentNeoBlock.Builder(this, "block").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlock block = this.getArgument(context, "block");
            WorldManager.removeNeoBlock(block);

            return success(context, "command.neoblock.remove_block");
        }
    }

    public static class Stop extends NeoCommand {
        protected Stop(NeoCommand parent) {
            super(parent, "block stop");
            new NeoArgumentNeoBlock.Builder(this, "block").provider(
                    NeoArgumentNeoBlock.createSuggester(b -> !b.isStopped())
            ).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlock block = this.getArgument(context, "block");
            if (!block.isStopped()) block.stop();

            Optional<Activate> activate_command = NeoCommand.getFromRegistry(Activate.class);
            Optional<RemoveBlock> remove_command = NeoCommand.getFromRegistry(RemoveBlock.class);
            return success(context, "command.neoblock.stopped", activate_command.map(NeoCommand::getCommand).orElse(null), remove_command.map(NeoCommand::getCommand).orElse(null));
        }
    }

    public static class Activate extends NeoCommand {
        protected Activate(NeoCommand parent) {
            super(parent, "block activate");
            new NeoArgumentNeoBlock.Builder(this, "block").provider(
                    NeoArgumentNeoBlock.createSuggester(NeoBlock::isStopped)
            ).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlock block = this.getArgument(context, "block");
            if (block.isStopped()) block.activate();

            return success(context, "command.neoblock.activated");
        }
    }
}