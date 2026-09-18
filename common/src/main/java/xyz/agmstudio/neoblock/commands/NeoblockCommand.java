package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.commands.NeoArgumentBlockPos;
import xyz.agmstudio.neocore.commands.NeoArgumentEntityType;
import xyz.agmstudio.neocore.commands.NeoArgumentInteger;
import xyz.agmstudio.neocore.commands.NeoCommand;

import java.util.ArrayList;
import java.util.List;

public class NeoblockCommand extends NeoCommand.ParentHolder {
    private static NeoblockCommand instance = null;
    public static NeoblockCommand getInstance(CommandBuildContext buildContext) {
        if (instance == null) instance = new NeoblockCommand(buildContext);
        return instance;
    }

    private NeoblockCommand(CommandBuildContext buildContext) {
        super(buildContext, "neoblock");

        new Help(this);
        new Home(this);
        new GiveMobTicket(this);
        new GetBlockId(this);

        new NeoblockForceCommand(this);
        new NeoblockSchematicCommand(this);
        new NeoblockTiersCommand(this);
        new NeoBlockCooldownCommand(this);
        new NeoblockTraderCommand(this);
    }

    public static class Help extends NeoCommand {
        protected Help(NeoCommand parent) {
            super(parent, "help");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            List<NeoCommand> commands = getListOf(instance);
            MutableComponent component = Component.literal("");
            int counter = 1;
            for (NeoCommand command: commands) {
                if (!command.permission.test(context.getSource())) continue;
                component.append("\n" + (counter++) + "- ").append(command.getDescription());
                component.append(":\n   ").append(command.getFullCommand());
            }
            return success(context, "command.neoblock.help", component);
        }
        private static List<NeoCommand> getListOf(NeoCommand command) {
            List<NeoCommand> commands = new ArrayList<>();
            if (!(command instanceof ParentHolder)) commands.add(command);
            for (NeoCommand subCommand: command.getSubCommands())
                commands.addAll(getListOf(subCommand));

            return commands;
        }
    }

    public static class Home extends NeoCommand {
        protected Home(NeoCommand parent) {
            super(parent, "home");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Entity entity = context.getSource().getEntityOrException();
            if (entity.level() instanceof ServerLevel server) {
                NeoBlock block = ForgivingVoid.findNearestBlock(server, entity);
                if (block == null) block = WorldManager.getBlocks().get(0);
                BlockPos pos = block.safeBlock();
                NeoMC.teleportEntity(entity, block.level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0, 0);
                return success(context,"command.neoblock.home", entity.getDisplayName());
            } else {
                return fail(context, "command.neoblock.home_not_found");
            }
        }
    }

    public static class GiveMobTicket extends NeoCommand {
        protected GiveMobTicket(NeoCommand parent) {
            super(parent, "get mobticket");
            new NeoArgumentEntityType.Builder(this, "entity").build(this.buildContext);
            new NeoArgumentInteger.Builder(this, "count").defaultValue(1).min(1).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            EntityType<?> type = this.getArgument(context, "entity");

            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();

            int count = this.getArgument(context, "count");
            ItemStack mob_ticket = NeoMobSpec.of(type, count);

            boolean added = player.getInventory().add(mob_ticket);
            if (!added) player.drop(mob_ticket, false);

            return success(context, "command.neoblock.mobticket", count, type.toShortString());
        }
    }

    public static class GetBlockId extends NeoCommand {
        protected GetBlockId(NeoCommand parent) {
            super(parent, "get id");
            new NeoArgumentBlockPos.Builder(this, "block").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            BlockPos pos = this.getArgument(context, "block");
            for (NeoBlock block: WorldManager.getBlocks())
                if (block.getBlockPos().equals(pos))
                    return success(context, "command.neoblock.get_block_id", block.getBlockPos().toShortString(), block.getID());

            return fail(context, "command.neoblock.get_block_not_found");
        }
    }
}