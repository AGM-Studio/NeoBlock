package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import xyz.agmstudio.neocore.commands.NeoArgumentInteger;
import xyz.agmstudio.neocore.commands.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.NeoBlockCooldown;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.util.StringUtil;

public class NeoBlockCooldownCommand extends NeoCommand {
    protected NeoBlockCooldownCommand(NeoCommand parent) {
        super(parent, "cooldown");

        new Advance(this);
        new AddCooldown(this);
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        NeoBlockCooldown cooldown = WorldManager.getWorldData().getCooldown();
        if (cooldown == null) return fail(context, "command.neoblock.cooldown.no_cooldown");
        return success(context, "command.neoblock.cooldown", cooldown.getType().id(), StringUtil.formatTicks(cooldown.getTick()), StringUtil.formatTicks(cooldown.getTime()));
    }

    public static class Advance extends NeoCommand {
        protected Advance(NeoCommand parent) {
            super(parent, "advance");
            new NeoArgumentInteger.Builder(this, "ticks").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            NeoBlockCooldown cooldown = WorldManager.getWorldData().getCooldown();
            if (cooldown == null) return fail(context, "command.neoblock.cooldown.no_cooldown");

            int value = this.getArgument(context, "ticks");
            long remain = cooldown.getTime() - cooldown.advanceBy(value);
            return success(context, "command.neoblock.cooldown.advance", StringUtil.formatTicks(remain));
        }
    }

    public static class AddCooldown extends NeoCommand {
        protected AddCooldown(NeoCommand parent) {
            super(parent, "add");
            new NeoArgumentInteger.Builder(this, "ticks").build();
        }

        @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            int value = this.getArgument(context, "ticks");
            NeoBlockCooldown.Type.Normal.create(value);

            return success(context, "command.neoblock.cooldown.add", StringUtil.formatTicks(value));
        }
    }
}