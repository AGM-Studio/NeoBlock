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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentEntityType;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentInteger;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
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

        new NeoblockForceCommand(this);
        new NeoblockSchematicCommand(this);
        new NeoblockTiersCommand(this);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        MutableComponent tiers = Component.literal("");
        for (TierSpec tier: WorldData.getWorldTiers()) {
            MutableComponent name = Component.literal(tier.getName());
            if (!tier.isResearched()) name.setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            else if (tier.isEnabled()) name.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            else name.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            tiers.append("\n    - ").append(name);
        }

        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getWorldStatus().getBlockCount(), tiers);
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    public static class Home extends NeoCommand {
        protected Home(NeoCommand parent) {
            super(parent, "home");
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
}
