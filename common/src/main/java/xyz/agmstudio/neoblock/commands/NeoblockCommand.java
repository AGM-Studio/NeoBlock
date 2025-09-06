package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentEntityType;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentInteger;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;
import xyz.agmstudio.neoblock.neo.loot.NeoMobSpec;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

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
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MutableComponent tiers = Component.literal("");
        for (TierSpec tier: WorldData.getWorldTiers()) {
            MutableComponent name = Component.literal(tier.getName());
            if (!tier.isResearched()) name.setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            else if (tier.isEnabled()) name.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            else name.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            tiers.append("\n    - ").append(name);
        }

        return success(context, "command.neoblock.info", WorldData.getWorldStatus().getBlockCount(), tiers);
    }

    public static class Home extends NeoCommand {
        protected Home(NeoCommand parent) {
            super(parent, "home");
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            Entity entity = context.getSource().getEntityOrException();

            NeoBlockPos.safeBlock().teleportTo(entity);
            return success(context,"command.neoblock.home", entity.getDisplayName());
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
