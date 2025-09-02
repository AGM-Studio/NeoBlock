package xyz.agmstudio.neoblock.mixincommon;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.lang.reflect.Field;
import java.util.List;

public final class CreateWorldScreenMixinCommon {
    private static WorldCreationUiState getUiState(Object screen) {
        for (Field field : screen.getClass().getDeclaredFields()) {
            if (field.getType().equals(WorldCreationUiState.class)) {
                try {
                    field.setAccessible(true);
                    return (WorldCreationUiState) field.get(screen);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot access uiState field", e);
                }
            }
        }

        throw new RuntimeException("Could not find WorldCreationUiState");
    }

    public static void changeWorldDefault(Object screen) {
        WorldCreationUiState uiState;
        try {
            uiState = getUiState(screen);
        } catch (RuntimeException e) {
            NeoBlock.LOGGER.error("Could not access the \"uiState\". Aborting the mixin!", e);
            return;
        }

        List<WorldCreationUiState.WorldTypeEntry> list = uiState.getNormalPresetList();
        String name = NeoBlock.getConfig().get(
                "world.preset",
                NeoBlock.getConfig().get("world.no-nether", true) ?
                        "neoblock:neoblock_no_nether" : "neoblock:neoblock"
        );

        ResourceLocation location = MinecraftUtil.parseResourceLocation(name);
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