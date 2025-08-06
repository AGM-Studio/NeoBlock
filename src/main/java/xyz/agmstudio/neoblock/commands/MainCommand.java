package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

public class MainCommand extends NeoCommand {
    public MainCommand() {
        super("neoblock");
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        MutableComponent tiers = Component.literal("");
        for (TierSpec tier: WorldData.getWorldTiers()) {
            if (!tier.isResearched()) continue;
            MutableComponent name = Component.literal(tier.getName());
            if (tier.isEnabled()) name.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            else name.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            tiers.append("\n    - ").append(name);
        }

        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getWorldStatus().getBlockCount(), tiers);
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }
}
