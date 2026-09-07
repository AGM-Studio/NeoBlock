package xyz.agmstudio.neocore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.agmstudio.neocore.platform.IConfig;
import xyz.agmstudio.neocore.platform.IMC;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


@ParametersAreNonnullByDefault
public abstract class NeoMod {
    protected static IMC MC = null;
    public static final Logger CORE_LOGGER = LoggerFactory.getLogger("NeoCore");
    public static boolean isNull(Object result) {
        return NeoMod.MC.isNull(result);
    }

    private final String id;
    private final String name;
    private final Logger logger;

    private IConfig config;

    public NeoMod(String id, String name) {
        this.id = id;
        this.name = name;

        this.logger = LoggerFactory.getLogger(name);
        this.config = getConfig("config.toml");
    }

    public String getModId() {
        return id;
    }
    public String getModName() {
        return name;
    }
    public Logger getModLogger() {
        return logger;
    }
    public IConfig getModConfig() {
        return config;
    }
    public void reloadModConfig() {
        this.config = getConfig("config.toml");
    }

    /**
     * Creates a config folder and returns the path to it using the path given
     *
     * @param paths the path to folder starting from the config folder
     * @return path to desired folder
     */
    public @NotNull Path getConfigFolder(String... paths) {
        Path path = NeoMC.getMainConfigFolder().resolve(this.name);
        for (String p: paths) path = path.resolve(p);
        if (!path.toFile().exists() && path.toFile().mkdirs()) logger.debug("Creating folder {}", path);
        return path;
    }

    /**
     * Returns the {@link IConfig} corresponding to name in folder.
     * If missing will try to load from resources.
     *
     * @param folder the folder to look up
     * @param config the config name
     * @return the config (see {@link IConfig})
     */
    public final @Nullable IConfig getConfig(Path folder, String config) {
        if (!folder.toFile().exists()) try {
            Files.createDirectories(folder);
        } catch (IOException ignored) {}
        Path configPath = folder.resolve(config.endsWith(".toml") ? config : config + ".toml");
        if (!Files.exists(configPath)) try {
            Path path = Path.of(NeoMC.getMainConfigFolder().toAbsolutePath().toString(), this.name);
            String resource = configPath.toAbsolutePath().toString().replace(path.toAbsolutePath().toString(), "\\configs");
            this.logger.debug("Loading resource {} for {}", resource, configPath);
            this.processResourceFile(resource, configPath, new HashMap<>());
        } catch (Exception ignored) {}
        if (!Files.exists(configPath)) return null;

        try {
            return NeoMod.MC.getConfig(configPath);
        } catch (Exception exception) {
            NeoMod.CORE_LOGGER.error("Unable to load config of {} from {}", config, folder);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Returns the {@link IConfig} corresponding to name in main folder.
     * If missing will try to load from resources.
     *
     * @param config the config name
     * @return the config (see {@link IConfig})
     */
    public final @Nullable IConfig getConfig(String config) {
        return getConfig(getConfigFolder(), config);
    }

    /**
     * Checks if a resource file exists in the classpath.
     *
     * @param resourcePath The path to the resource file within the classpath.
     * @return true if the resource exists; false otherwise.
     */
    public boolean doesResourceExist(String resourcePath) {
        try (InputStream resourceStream = this.getClass().getResourceAsStream(resourcePath)) {
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
    public void processResourceFile(String resourcePath, Path outputPath, Map<String, String> placeholders) throws IOException {
        logger.debug("Processing resource {} to {}", resourcePath, outputPath.toAbsolutePath());
        try (InputStream inputStream = this.getClass().getResourceAsStream(resourcePath.replace("\\", "/"));
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
             BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(replacePlaceholders(line, placeholders));
                writer.newLine();
            }
        }
    }
    private static String replacePlaceholders(String line, Map<String, String> placeholders) {
        AtomicReference<String> modifiedLine = new AtomicReference<>(line);
        placeholders.forEach((key, value) -> modifiedLine.set(modifiedLine.get().replace(key, value)));
        return modifiedLine.get();
    }
}