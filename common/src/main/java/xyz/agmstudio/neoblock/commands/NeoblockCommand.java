package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import xyz.agmstudio.neocore.commands.NeoArgumentEntityType;
import xyz.agmstudio.neocore.commands.NeoArgumentInteger;
import xyz.agmstudio.neocore.commands.NeoCommand;

public class NeoblockCommand extends NeoCommand {
    private static NeoblockCommand instance = null;
    public static NeoblockCommand getInstance(CommandBuildContext buildContext) {
        if (instance == null) instance = new NeoblockCommand(buildContext);
        return instance;
    }

    private NeoblockCommand(CommandBuildContext buildContext) {
        super(buildContext, "neoblock");

        new Home(this);
        new GiveMobTicket(this);

        new NeoblockForceCommand(this);
        new NeoblockSchematicCommand(this);
        new NeoblockTiersCommand(this);
        new NeoBlockCooldownCommand(this);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MutableComponent component = Component.literal("");
        int blockCounter = 0;
        for (NeoBlock block: WorldManager.getBlocks()) {
            MutableComponent blockText = Component.literal("(" + block.getBlockPos().toShortString() + "@" + block.getDimension().dimension().location() + "): " + block.getBlockCount());
            if (block.isOnCooldown()) blockText.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            else blockText.setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            component.append("    " + (++blockCounter) + "- ").append(blockText).append("\n");
        }

        return success(context, "command.neoblock.info", component);
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
                BlockPos pos = block.getBlockPos();
                NeoMC.teleportEntity(entity, block.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
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
}