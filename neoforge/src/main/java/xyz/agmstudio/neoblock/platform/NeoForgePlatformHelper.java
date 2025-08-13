package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import xyz.agmstudio.neoblock.platform.helpers.IPlatformHelper;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements IPlatformHelper {
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
        BiFunction<CompoundTag, HolderLookup.Provider, T> neoLoader = ((tag, provider) -> loader.apply(tag));
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(creator, neoLoader), name);
    }
}