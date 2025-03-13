package xyz.agmstudio.neoblock.tiers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.config.Config;
import xyz.agmstudio.neoblock.util.MessagingUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class NeoTier {
    protected final static Path PATH = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), NeoBlockMod.MOD_ID + "/tiers.toml");
    protected final static CommentedFileConfig CONFIG = CommentedFileConfig.builder(PATH).sync().build();

    public static void reload() {
        CONFIG.load();

        NeoBlock.TIERS.clear();
        for (int i = 0; i < Config.tiers.get(); i++)
            NeoBlock.TIERS.add(new NeoTier(i));

        CONFIG.save();
    }

    @SuppressWarnings("FieldMayBeFinal")
    private List<BlockState> BLOCKS;
    public final int TIER;
    private int WEIGHT;
    private int UNLOCK;
    private NeoTrade UNLOCK_TRADE = null;

    private String getPath(String key) {
        return "tier-" + TIER + "." + key;
    }

    protected NeoTier(int tier) {
        this.TIER = tier;

        String path = getPath("unlock");
        if (CONFIG.contains(path)) UNLOCK = CONFIG.getInt(path);
        else {
            UNLOCK = 100 * TIER;
            CONFIG.setComment(path, "Amount of blocks broken to unlock.");
            CONFIG.set(path, UNLOCK);
        }
        path = getPath("weight");
        if (CONFIG.contains(path)) WEIGHT = CONFIG.getInt(path);
        else {
            WEIGHT = 1;
            CONFIG.setComment(path, "The wight of tier when choosing between unlocked tiers.");
            CONFIG.set(path, WEIGHT);
        }
        path = getPath("blocks");
        if (CONFIG.contains(path)) {
            List<String> blocks = CONFIG.get(path);
            BLOCKS = blocks.stream().map(block -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)).defaultBlockState()).toList();
        } else {
            BLOCKS = List.of(Blocks.GRASS_BLOCK.defaultBlockState());
            CONFIG.setComment(path, "List of all blocks in this tier. All of them have equal chance to be chosen in this tier.");
            CONFIG.set(path, getBlocksName());
        }
        path = getPath("unlock-trades");
        if (CONFIG.contains(path)) {
            List<String> trades = CONFIG.get(path);
            UNLOCK_TRADE = NeoTrade.parse(trades);
        }
    }

    public int getUnlock() {
        return UNLOCK;
    }
    public int getWeight() {
        return WEIGHT;
    }
    public List<BlockState> getBlocks() {
        return BLOCKS.stream().toList();
    }
    public List<String> getBlocksName() {
        return BLOCKS.stream().map(block -> BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString()).toList();
    }
    public void setUnlock(int unlock) {
        UNLOCK = unlock;
        CONFIG.set(getPath("unlock"), UNLOCK);
    }
    public void setWeight(int weight) {
        WEIGHT = weight;
        CONFIG.set(getPath("weight"), WEIGHT);
    }
    public void addBlock(BlockState block) {
        BLOCKS.add(block);
        CONFIG.set(getPath("blocks"), getBlocksName());
    }
    public void removeBlock(BlockState block) {
        BLOCKS.remove(block);
        CONFIG.set(getPath("blocks"), getBlocksName());
    }

    public void checkScore(Level level) {
        if (NeoBlock.BLOCK_SCOREBOARD.getScore("Total") == UNLOCK) {  // On unlock
            if (UNLOCK_TRADE != null) UNLOCK_TRADE.spawnTrader(level);
            MessagingUtil.sendMessage("messages.neoblock.unlocked_tier", level, TIER);
        }
    }
}
