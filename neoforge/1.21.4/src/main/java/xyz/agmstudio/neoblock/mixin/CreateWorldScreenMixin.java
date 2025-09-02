package xyz.agmstudio.neoblock.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldCallback;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.agmstudio.neoblock.mixincommon.CreateWorldScreenMixinCommon;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalLong;

@Mixin(CreateWorldScreen.class)
public final class CreateWorldScreenMixin {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void CreateWorldScreen(Minecraft minecraft, @Nullable Screen lastScreen, WorldCreationContext context, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed, CreateWorldCallback createWorldCallback, CallbackInfo ci) {
        CreateWorldScreenMixinCommon.changeWorldDefault(this);
    }

    // No Expremental notice
    @ModifyArg(method = "onCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;confirmWorldCreation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"), index = 2)
    private Lifecycle modifyLifecycle(Lifecycle original) {
        if (original == Lifecycle.experimental()) return Lifecycle.stable();
        return original;
    }
}