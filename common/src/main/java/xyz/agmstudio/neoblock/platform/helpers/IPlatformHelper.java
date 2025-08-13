package xyz.agmstudio.neoblock.platform.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IPlatformHelper {
    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Returns the config folder of the loader
     *
     * @return path of the config folder of the loader
     */
    Path getConfigFolder();

    /**
     * Captures a saved data from the level
     *
     * @param level a Server level to load from
     * @param name the name of saved data
     * @param loader the function to load existing data
     * @param creator the function to create a new version
     * @return the saved date
     */
    <T extends SavedData> T captureSavedData(ServerLevel level, String name, Function<CompoundTag, T> loader, Supplier<T> creator);

    /**
     * Creates a new instance of world data subclass for the loader
     *
     * @param level the level for the constructor
     * @return just instance of a subclass of World data
     */
    WorldData instanceWorldData(ServerLevel level);
}