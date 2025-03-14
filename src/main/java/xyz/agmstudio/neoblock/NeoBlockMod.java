package xyz.agmstudio.neoblock;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.moddingx.libx.mod.ModXRegistration;
import xyz.agmstudio.neoblock.data.Config;
import xyz.agmstudio.neoblock.tiers.NeoBlock;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

@Mod(NeoBlockMod.MOD_ID)
public final class NeoBlockMod extends ModXRegistration {
    private static NeoBlockMod instance;
    public static NeoBlockMod getInstance() {
        return instance;
    }

    public static final String MOD_ID = "neoblock";
    public static final Logger LOGGER = LogManager.getLogger();
    public static ModContainer CONTAINER;

    public NeoBlockMod(ModContainer container) {
        instance = this;
        NeoBlockMod.CONTAINER = container;

        try {
            Files.createDirectory(Config.CONFIG_PATH);
        } catch (FileAlreadyExistsException e) {
            NeoBlockMod.LOGGER.debug("Config directory " + NeoBlockMod.MOD_ID + " already exists. Skip creating.");
        } catch (IOException e) {
            NeoBlockMod.LOGGER.error("Failed to create " + NeoBlockMod.MOD_ID + " config directory", e);
        }

        container.registerConfig(ModConfig.Type.COMMON, Config.CLIENT_SPEC, NeoBlockMod.MOD_ID + "/config.toml");

        NeoForge.EVENT_BUS.addListener(MessagingUtil::onPlayerJoin);
    }

    @Override
    protected void setup(FMLCommonSetupEvent event) {
        NeoBlock.reload();
    }
    @Override
    protected void clientSetup(FMLClientSetupEvent event) {}
}