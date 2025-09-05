package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBoolean;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentInteger;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentTier;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.tiers.TierResearch;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.StringUtil;

public class NeoblockTiersCommand extends NeoCommand.ParentHolder {
    protected NeoblockTiersCommand(NeoCommand parent) {
        super(parent, "tiers", 3);

        new Satisfy(this);
        new Enable(this);
        new Disable(this);
        new AdvanceResearch(this);
    }

    public static class Satisfy extends NeoCommand {
        protected Satisfy(NeoCommand parent) {
            super(parent, "satisfy");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> !tier.hasSpecialRequirement() && !tier.isResearched()))
                    .build();
            new NeoArgumentBoolean.Builder(this, "force").defaultValue(true).build();
        }
    
        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            TierSpec tier = this.getArgument(context, "tier", TierSpec.class);
            boolean force = getArgument(context, "force", Boolean.class);
    
            WorldData.setCommanded(tier, force);
            return success(context, "command.neoblock.unlock_tier");
        }
    }

    public static class Disable extends NeoCommand {
        protected Disable(NeoCommand parent) {
            super(parent, "disable");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> tier.isEnabled() && tier.isResearched()))
                    .build();
        }
    
        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            this.getArgument(context, "tier", TierSpec.class).disable();
            return success(context, "command.neoblock.disable_tier");
        }
    }

    public static class Enable extends NeoCommand {
        protected Enable(NeoCommand parent) {
            super(parent, "enable");
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> !tier.isEnabled() && tier.isResearched()))
                    .build();
        }
    
        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            this.getArgument(context, "tier", TierSpec.class).enable();
            return success(context, "command.neoblock.enable_tier");
        }
    }

    public static class AdvanceResearch extends NeoCommand {
        protected AdvanceResearch(NeoCommand parent) {
            super(parent, "research advance");
            new NeoArgumentInteger.Builder(this, "ticks").build();
            new NeoArgumentTier.Builder(this, "tier")
                    .provider(NeoArgumentTier.createSuggester(tier -> tier.canBeResearched() && !tier.isResearched()))
                    .defaultValue(null).build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            TierSpec tier = this.getArgument(context, "tier", TierSpec.class);

            TierResearch research = null;
            if (tier == null) research = TierManager.fetchCurrentResearch();
            else if (!tier.isResearched() && tier.canBeResearched()) research = tier.getResearch();

            if (research == null) {
                context.getSource().sendFailure(Component.translatable("command.neoblock.research.advance.invalid_tier"));
                return 0;
            }

            int value = this.getArgument(context, "ticks");
            long remain = research.getTime() - research.advanceBy(value);
            return success(context, "command.neoblock.research.advance", StringUtil.formatTicks(remain));
        }
    }
}
