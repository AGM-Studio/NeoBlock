package xyz.agmstudio.neoblock.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceUtil {
    private static final Class<?> clazz = NeoBlockMod.class;

    /**
     * Checks if a resource file exists in the classpath.
     *
     * @param resourcePath The path to the resource file within the classpath.
     * @return true if the resource exists; false otherwise.
     */
    public static boolean doesResourceExist(String resourcePath) {
        try (InputStream resourceStream = clazz.getResourceAsStream(resourcePath)) {
            return resourceStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Copies a resource file, replaces placeholders, and saves it to the specified path.
     *
     * @param resourcePath The path to the resource file within the mod's resources.
     * @param outputPath   The path where the modified file will be saved.
     * @param placeholders A map of placeholders and their replacement values.
     * @throws IOException If an I/O error occurs.
     */
    public static void processResourceFile(String resourcePath, Path outputPath, Map<String, String> placeholders) throws IOException {
        NeoBlockMod.LOGGER.debug("Processing resource {} to {}", resourcePath, outputPath.toAbsolutePath());
        try (InputStream inputStream = clazz.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
             BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(replacePlaceholders(line, placeholders));
                writer.newLine();
            }
        }
    }

    /**
     * Replaces placeholders in a line with their corresponding values.
     *
     * @param line         The line containing placeholders.
     * @param placeholders A map of placeholders and their replacement values.
     * @return The line with placeholders replaced.
     */
    private static String replacePlaceholders(String line, Map<String, String> placeholders) {
        AtomicReference<String> modifiedLine = new AtomicReference<>(line);
        placeholders.forEach((key, value) -> modifiedLine.set(modifiedLine.get().replace(key, value)));
        return modifiedLine.get();
    }

    /**
     * Returns the {@link CommentedFileConfig} corresponding to name in folder.
     * If missing will try to load from resources.
     *
     * @param folder the folder to look up
     * @param name the config name
     * @return the config (see {@link CommentedFileConfig}
     */
    public static CommentedFileConfig getConfig(Path folder, String name) {
        if (!folder.toFile().exists()) try {
            Files.createDirectories(folder);
        } catch (IOException ignored) {}
        Path configPath = folder.resolve(name.endsWith(".toml") ? name : name + ".toml");
        if (!Files.exists(configPath)) try {
            Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID);
            String resource = configPath.toAbsolutePath().toString().replace(path.toAbsolutePath().toString(), "/");
            NeoBlockMod.LOGGER.debug("Loading resource {} for {}", resource, configPath);
            processResourceFile(resource, configPath, new HashMap<>());
        } catch (IOException ignored) {}
        if (!Files.exists(configPath)) return null;

        CommentedFileConfig config = CommentedFileConfig.builder(configPath).sync().build();

        config.load();
        return config;
    }

    /**
     * Creates a config folder and returns the path to it using the path given
     *
     * @param paths the path to folder starting from the config folder
     * @return path to desired folder
     */
    public static @NotNull Path getConfigFolder(String... paths) {
        Path path = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), paths);
        if (!path.toFile().exists() && path.toFile().mkdirs()) NeoBlockMod.LOGGER.debug("Creating folder {}", path);
        return path;
    }
}
