package xyz.agmstudio.neoblock.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentBlockPos;
import xyz.agmstudio.neoblock.commands.util.NeoArgumentDimension;
import xyz.agmstudio.neoblock.commands.util.NeoCommand;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.world.WorldData;

public class ForceBlockCommand extends NeoCommand {
    public ForceBlockCommand() {
        super("neoblock force setblock", 4);
        new NeoArgumentBlockPos.Builder(this, "pos").build();
        new NeoArgumentDimension.Builder(this, "dimension").defaultValue(null).build();
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) throws CommandExtermination {
        CommandSourceStack source = context.getSource();
        BlockPos origin = getArgument(context, "pos");
        ServerLevel world = getArgument(context, "dimension", source::getLevel);

        WorldData.getWorldStatus().setBlockPos(origin, WorldData.getWorldLevel());
        WorldData.getWorldStatus().setDimension(world);
        if (WorldData.getWorldStatus().isDisabled()) {
            WorldData.getWorldStatus().setActive();
            source.sendSuccess(() -> Component.translatable("command.neoblock.force_block.enabled"), true);
        } else source.sendSuccess(() -> Component.translatable("command.neoblock.force_block"), true);
        BlockManager.updateBlock(WorldData.getWorldLevel(), false);

        return 1;
    }
}
