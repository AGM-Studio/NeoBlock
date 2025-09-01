package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBoolean;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

public class NeoblockTiersCommand extends NeoCommand {
    public NeoblockTiersCommand(NeoCommand parent) {
        super(parent, "tiers", 3);

        new Satisfy(this);
        new Enable(this);
        new Disable(this);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.tiers"), false);
        return 1;
    }

    public static class Satisfy extends NeoCommand {
        public Satisfy(NeoCommand parent) {
            super(parent, "satisfy");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> !tier.hasSpecialRequirement() && !tier.isResearched()))
                    .build();
            new NeoArgumentBoolean.Builder(this, "force").defaultValue(true).build();
        }
    
        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            TierSpec tier = this.getArgument(context, "tier", TierSpec.class);
            boolean force = getArgument(context, "force", Boolean.class);
    
            WorldData.setCommanded(tier, force);
            context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.unlock_tier"), false);
            return 1;
        }
    }

    public static class Disable extends NeoCommand {
        public Disable(NeoCommand parent) {
            super(parent, "disable");
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

    public static class Enable extends NeoCommand {
        public Enable(NeoCommand parent) {
            super(parent, "enable");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> !tier.isEnabled() && tier.isResearched()))
                    .build();
        }
    
        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            this.getArgument(context, "tier", TierSpec.class).enable();
            context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.enable_tier"), false);
            return 1;
        }
    }
}
