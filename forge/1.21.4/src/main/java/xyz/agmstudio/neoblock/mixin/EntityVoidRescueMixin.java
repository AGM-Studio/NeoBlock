package xyz.agmstudio.neoblock.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;

@Mixin(Entity.class)
public abstract class EntityVoidRescueMixin {
    @Inject(method = "discard()V", at = @At("HEAD"), cancellable = true)
    private void onEntityDiscard(CallbackInfo ci) {
        //noinspection DataFlowIssue
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel level && self.getY() < level.getMinY())
            if (ForgivingVoid.handleVoid(level, self)) ci.cancel();
    }
}
