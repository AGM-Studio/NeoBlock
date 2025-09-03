package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.ConfigUtil;
import xyz.agmstudio.neoblock.util.MessengerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NeoSeqBlockSpec extends NeoBlockSpec {
    private static final Pattern PATTERN = Pattern.compile("^(?:(?<count>\\d+)x *)?neoblock:(?:seq|sequence):(?<id>[^ ]+)$");

    private static final HashMap<String, List<NeoBlockSpec>> SEQUENCES = new HashMap<>();
    private final List<NeoBlockSpec> blocks;
    private final String id;

    public static List<NeoBlockSpec> getSequenceBlocks(String name) {
        return SEQUENCES.getOrDefault(name, List.of());
    }

    public static void reloadSequences() {
        IConfig config = ConfigUtil.getConfig(NeoBlock.CONFIG_FOLDER, "sequences");
        if (config == null) {
            NeoBlock.LOGGER.error("Failed to load block sequences config.");
            return;
        }

        SEQUENCES.clear();

        for (String key : config.valueMap().keySet()) {
            List<NeoBlockSpec> list = extractSequenceList(config.get(key));
            if (list.isEmpty()) NeoBlock.LOGGER.info("Unable to load sequence {} because it's empty.", key);
            else {
                NeoBlock.LOGGER.info("Loaded {} blocks for {}.", list.size(), key);
                SEQUENCES.put(key, list);
            }
        }

        NeoBlock.LOGGER.info("Loaded {} block sequences.", SEQUENCES.size());
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
        if (blocks.isEmpty()) NeoBlock.LOGGER.warn("Unknown sequence ID: '{}'", matcher.group("id"));

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
        if (blocks.isEmpty()) {
            MessengerUtil.warnPlayers(level, "Unable to place {} because it's empty. Capturing a random block again.", getID());
            BlockManager.getRandomBlock().placeAt(level, pos);
            return;
        }

        super.placeAt(level, pos);  // Place the first block

        addToQueue(true);
    }

    @Override public NeoBlockSpec copy() {
        return copy(weight);
    }
    @Override public NeoBlockSpec copy(int weight) {
        return new NeoSeqBlockSpec(blocks, weight, id);
    }

    @Override public String toString() {
        return getID() + "{" + blocks.stream().map(Object::toString).collect(Collectors.joining(";")) + "}";
    }
}
