package xyz.agmstudio.neoblock.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow @Final WorldCreationUiState uiState;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void CreateWorldScreen(Minecraft minecraft, Screen lastScreen, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed, CallbackInfo ci) {
        List<WorldCreationUiState.WorldTypeEntry> list = uiState.getNormalPresetList();
        ResourceLocation location = NeoBlockMod.getConfig().getOrElse("world.no-nether", true) ? ResourceLocation.parse("neoblock:neoblock_no_nether") : ResourceLocation.parse("neoblock:neoblock");

        WorldCreationUiState.WorldTypeEntry type = uiState.getWorldType();
        for (WorldCreationUiState.WorldTypeEntry entry : list) {
            Holder<WorldPreset> world = entry.preset();
            if (world == null) continue;
            if (world.is(location)) {
                type = entry;
                break;
            }
        }

        uiState.setWorldType(type);
    }
}