package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBoolean;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

public class CommandTierCommand extends NeoCommand {
    public CommandTierCommand() {
        super("neoblock unlock", 3);
        new NeoArgumentTier.Builder(this, "tier")
                .provider(NeoArgumentTier.createSuggester(tier -> !tier.isCommanded() && !tier.isUnlocked()))
                .build();
        new NeoArgumentBoolean.Builder(this, "force").defaultValue(true).build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        WorldTier tier = this.getArgument(context, "tier", WorldTier.class);
        boolean force = getArgument(context, "force", Boolean.class);

        WorldData.setCommanded(tier, force);
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.unlock_tier"), false);
        return 1;
    }
}
