package xyz.agmstudio.neoblock.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.helpers.IPlatformHelper;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override public String getPlatformName() {
        return "NeoForge";
    }

    @Override public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override public <T extends SavedData> T captureSavedData(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator) {
        return level.getDataStorage().computeIfAbsent(loader, creator, name);
    }

    @Override
    public WorldData instanceWorldData(ServerLevel level) {
        return new WorldData(level) {
            @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
                return saveDataOnTag(tag);
            }
        };
    }
}