package xyz.agmstudio.neoblock.mixin;

import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EndPortalFrameBlock.class)
public class EndPortalFrameBlockMixin {
    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static BlockBehaviour.Properties modifyProperties(BlockBehaviour.Properties properties) {
        return properties.strength(3.5F, 1200.0F); // like stone
    }
}