package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;

import java.util.stream.Collectors;

public class MainCommand extends NeoCommand {
    public MainCommand() {
        super("neoblock");
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        MutableComponent message = Component.translatable("command.neoblock.info", WorldData.getWorldStatus().getBlockCount(), WorldData.getWorldTiers().stream().filter(WorldTier::isUnlocked).map(WorldTier::getName).collect(Collectors.joining("\n\t")));

        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }
}
