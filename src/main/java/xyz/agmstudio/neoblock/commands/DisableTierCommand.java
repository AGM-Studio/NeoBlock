package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

public class DisableTierCommand extends NeoCommand {
    public DisableTierCommand() {
        super("neoblock disable", 3);
        new NeoArgumentTier.Builder(this, "tier")
                .provider(NeoArgumentTier.createSuggester(tier -> tier.isEnabled() && tier.isUnlocked()))
                .build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        this.getArgument(context, "tier", WorldTier.class).disable();
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.disable_tier"), false);
        return 1;
    }
}
