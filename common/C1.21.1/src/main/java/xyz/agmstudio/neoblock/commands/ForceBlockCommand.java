package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;

public class ForceBlockCommand extends NeoCommand {
    public ForceBlockCommand() {
        super("neoblock force setblock", 4);
        new NeoArgumentBlockPos.Builder(this, "pos").build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        BlockPos origin = getArgument(context, "pos");

        WorldData.getWorldStatus().setBlockPos(origin, WorldData.getWorldLevel());
        if (WorldData.getWorldStatus().isDisabled()) {
            WorldData.getWorldStatus().setActive();
            source.sendSuccess(() -> Component.translatable("command.neoblock.force_block.enabled"), true);
        } else source.sendSuccess(() -> Component.translatable("command.neoblock.force_block"), true);
        BlockManager.updateBlock(WorldData.getWorldLevel(), false);

        return 1;
    }
}
