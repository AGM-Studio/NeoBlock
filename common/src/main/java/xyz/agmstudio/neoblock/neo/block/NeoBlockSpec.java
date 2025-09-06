package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoBlockSpec {
    private static final Pattern PATTERN = Pattern.compile("^(?:(?<count>\\d+)x *)?(?<id>[a-z0-9_]+:[a-z0-9_/]+)$");
    protected static Block getDefault() {
        return BlockManager.DEFAULT_SPEC.getBlock();
    }

    protected final Block block;
    protected final int weight;

    public static Optional<? extends NeoBlockSpec> parse(String input) {
        Optional<NeoSeqBlockSpec> seq = NeoSeqBlockSpec.parseSequence(input);
        if (seq.isPresent()) return seq;

        Optional<NeoChestSpec> chest = NeoChestSpec.parseChest(input);
        if (chest.isPresent()) return chest;

        Optional<NeoTagBlockSpec> tag = NeoTagBlockSpec.parseTagBlock(input);
        if (tag.isPresent()) return tag;

        Matcher matcher = PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            NeoBlock.LOGGER.warn("Invalid block: '{}'", input);
            return Optional.empty();
        }

        Optional<Block> block = MinecraftUtil.getBlock(matcher.group("id"));
        if (block.isEmpty()) {
            NeoBlock.LOGGER.warn("Unknown block ID: '{}'", matcher.group("id"));
            return Optional.empty();
        }

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new NeoBlockSpec(block.get(), count));
    }

    public NeoBlockSpec(Block block, int weight) {
        this.block = block;
        this.weight = weight;
    }

    public NeoBlockSpec(Block block) {
        this(block, 1);
    }

    public Block getBlock() {
        return block;
    }
    public BlockState getState() {
        return getBlock().defaultBlockState();
    }
    public int getWeight() {
        return weight;
    }
    public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + MinecraftUtil.getBlockResource(getBlock()).orElse(null);
    }

    public void placeAt(@NotNull LevelAccessor level, BlockPos pos) {
        level.setBlock(pos, getState(), 3);
        BlockManager.ensureNoFall(level);
    }

    public NeoBlockSpec copy() {
        return new NeoBlockSpec(block, weight);
    }
    public NeoBlockSpec copy(int weight) {
        return new NeoBlockSpec(block, weight);
    }

    @Override public String toString() {
        return getID();
    }
}
