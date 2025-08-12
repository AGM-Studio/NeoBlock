package xyz.agmstudio.neoblock.util;

import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neoblock.platform.Services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceUtil {
    private static final Class<?> clazz = NeoBlock.class;

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
        NeoBlock.LOGGER.debug("Processing resource {} to {}", resourcePath, outputPath.toAbsolutePath());
        try (InputStream inputStream = clazz.getResourceAsStream(resourcePath.replace("\\", "/"));
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
     * Creates a config folder and returns the path to it using the path given
     *
     * @param paths the path to folder starting from the config folder
     * @return path to desired folder
     */
    public static @NotNull Path getConfigFolder(String... paths) {
        Path path = pathOf(paths);
        if (!path.toFile().exists() && path.toFile().mkdirs()) NeoBlock.LOGGER.debug("Creating folder {}", path);
        return path;
    }

    /**
     * Returns the path from the config folder
     *
     * @param paths the subdirectories to navigate
     * @return the path created
     */
    public static @NotNull Path pathOf(String... paths) {
        return Path.of(Services.PLATFORM.getConfigFolder().toAbsolutePath().toString(), paths);
    }

    /**
     * Loads all available tier configuration files from resources if they do not exist.
     */
    public static void loadAllTierConfigs() {
        // If tier-0.toml is present, no need to proceed
        if (Files.exists(TierSpec.FOLDER.resolve("tier-0.toml"))) return;
        if (TierSpec.FOLDER.toFile().mkdirs())
            NeoBlock.LOGGER.debug("Created config folder: {}", TierSpec.FOLDER);

        int tier = 0;
        while (true) {
            Path location = TierSpec.FOLDER.resolve("tier-" + tier + ".toml");
            String resource = "/configs/tiers/tier-" + tier + ".toml";
            Map<String, String> map = Map.of("[TIER]", Integer.toString(tier ++));

            if (Files.exists(location)) continue;
            if (!doesResourceExist(resource)) break;

            try {
                processResourceFile(resource, location, map);
                NeoBlock.LOGGER.debug("Loaded tier config from resource: {}", resource);
            } catch (IOException e) {
                NeoBlock.LOGGER.error("Unable to process resource {}", resource, e);
                break;
            }
        }

        Path templateLocation = TierSpec.FOLDER.resolve("tier-template.toml");
        if (!Files.exists(templateLocation)) {
            try {
                processResourceFile("/configs/tiers/tier-template.toml", templateLocation, Map.of("[TIER]", "10"));
                NeoBlock.LOGGER.debug("Loaded tier template config.");
            } catch (IOException e) {
                NeoBlock.LOGGER.error("Unable to process tier template resource", e);
            }
        }
    }
}
