package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;

public class DisableTierCommand extends NeoCommand {
    public DisableTierCommand() {
        super("neoblock disable", 3);
        new NeoArgumentTier.Builder(this, "tier")
                .provider(NeoArgumentTier.createSuggester(tier -> tier.isEnabled() && tier.isResearched()))
                .build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        this.getArgument(context, "tier", TierSpec.class).disable();
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.disable_tier"), false);
        return 1;
    }
}
