package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBoolean;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentInteger;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.tiers.TierResearch;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.StringUtil;

public class NeoblockTiersCommand extends NeoCommand {
    protected NeoblockTiersCommand(NeoCommand parent) {
        super(parent, "tiers", 3);

        new Satisfy(this);
        new Enable(this);
        new Disable(this);
        new AdvanceResearch(this);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.tiers"), false);
        return 1;
    }

    public static class Satisfy extends NeoCommand {
        protected Satisfy(NeoCommand parent) {
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
        protected Disable(NeoCommand parent) {
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
        protected Enable(NeoCommand parent) {
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

    public static class AdvanceResearch extends NeoCommand {
        protected AdvanceResearch(NeoCommand parent) {
            super(parent, "research advance");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> tier.canBeResearched() && !tier.isResearched()))
                    .build();
            new NeoArgumentInteger.Builder(this, "ticks").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
            TierSpec tier = this.getArgument(context, "tier", TierSpec.class);
            if (tier.isResearched() || !tier.canBeResearched()) {
                context.getSource().sendFailure(Component.translatable("command.neoblock.research.advance.invalid_tier"));
                return 0;
            }

            TierResearch research = tier.getResearch();
            int value = this.getArgument(context, "ticks");
            long remain = research.getTime() - research.advanceBy(value);
            context.getSource().sendSuccess(() -> Component.translatable("command.neoblock.research.advance", StringUtil.formatTicks(remain)), false);
            return 1;
        }
    }
}
