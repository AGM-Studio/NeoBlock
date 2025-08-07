package xyz.agmstudio.neoblock.neo.block;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.compatibility.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoSeqBlockSpec extends NeoBlockSpec {
    private static final Pattern PATTERN = Pattern.compile("^(?:(?<count>\\d+)x *)?neoblock:(?:seq|sequence):(?<id>[^ ]+)$");

    private static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID);
    private static final HashMap<String, List<NeoBlockSpec>> SEQUENCES = new HashMap<>();
    private final List<NeoBlockSpec> blocks;
    private final String id;

    public static List<NeoBlockSpec> getChestItems(String name) {
        return SEQUENCES.getOrDefault(name, List.of());
    }

    public static void reloadSequences() {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "sequences");
        if (config == null) {
            NeoBlockMod.LOGGER.error("Failed to load block sequences config.");
            return;
        }

        SEQUENCES.clear();

        for (String key : config.valueMap().keySet()) {
            List<NeoBlockSpec> list = extractSequenceList(config.get(key));
            if (list.isEmpty()) NeoBlockMod.LOGGER.info("Unable to load sequence {} because it's empty.", key);
            else {
                NeoBlockMod.LOGGER.info("Loaded {} blocks for {}.", list.size(), key);
                SEQUENCES.put(key, list);
            }
        }

        NeoBlockMod.LOGGER.info("Loaded {} block sequences.", SEQUENCES.size());
    }

    public static List<NeoBlockSpec> extractSequenceList(List<String> entries) {
        if (entries == null) return List.of();

        List<NeoBlockSpec> list = new ArrayList<>();
        entries.forEach(entry -> NeoBlockSpec.parse(entry).ifPresent(list::add));
        return list;
    }

    public static Optional<NeoSeqBlockSpec> parseSequence(String input) {
        Matcher matcher = PATTERN.matcher(input.trim());
        if (!matcher.matches()) return Optional.empty();

        List<NeoBlockSpec> blocks = SEQUENCES.getOrDefault(matcher.group("id"), List.of());
        if (blocks.isEmpty()) NeoBlockMod.LOGGER.warn("Unknown sequence ID: '{}'", matcher.group("id"));

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new NeoSeqBlockSpec(blocks, count, matcher.group("id")));
    }

    public NeoSeqBlockSpec(List<NeoBlockSpec> blocks, int weight, String id) {
        super(Blocks.BEDROCK, weight);

        this.blocks = blocks;
        this.id = id;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public void addToQueue(boolean removeFirst) {
        if (removeFirst) {
            NeoBlockSpec first = blocks.remove(0);
            if (first.weight > 1) blocks.add(0, first.copy(first.weight - 1));
        }

        blocks.forEach(block -> {
            for (int i = 0; i < block.weight; i++)
                WorldData.getWorldStatus().addToQueue(block.copy(1));
        });
    }

    @Override public Block getBlock() {
        return blocks.get(0).block;
    }

    @Override public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + "neoblock:sequence:" + id;
    }

    @Override public void placeAt(@NotNull LevelAccessor level, BlockPos pos) {
        super.placeAt(level, pos);  // Place the first block

        addToQueue(true);
    }
}
