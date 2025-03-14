package xyz.agmstudio.neoblock.util;

import xyz.agmstudio.neoblock.NeoBlockMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceUtil {
    /**
     * Checks if a resource file exists in the classpath.
     *
     * @param resourcePath The path to the resource file within the classpath.
     * @return true if the resource exists; false otherwise.
     */
    public static boolean doesResourceExist(String resourcePath) {
        try (InputStream resourceStream = NeoBlockMod.class.getResourceAsStream(resourcePath)) {
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
        try (InputStream inputStream = NeoBlockMod.class.getResourceAsStream(resourcePath);
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
}
