package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoBlockSpec {
    private static final Pattern BLOCK_PATTERN = Pattern.compile("^(?:(?<count>\\d+)x)?(?<id>[a-z0-9_]+:[a-z0-9_/]+)$");

    private final Block block;
    private final int weight;

    public NeoBlockSpec(Block block, int weight) {
        this.block = block;
        this.weight = weight;
    }

    public NeoBlockSpec(Block block) {
        this(block, 1);
    }

    public static Optional<? extends NeoBlockSpec> parse(String input) {
        Optional<NeoChestSpec> chest = NeoChestSpec.parseChest(input);
        if (chest.isPresent()) return chest;

        Matcher matcher = BLOCK_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            NeoBlockMod.LOGGER.warn("Invalid block: '{}'", input);
            return Optional.empty();
        }

        Optional<Block> block = MinecraftAPI.getBlock(matcher.group("id"));
        if (block.isEmpty()) {
            NeoBlockMod.LOGGER.warn("Unknown block ID: '{}'", matcher.group("id"));
            return Optional.empty();
        }

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new NeoBlockSpec(block.get(), count));
    }

    public BlockState getState() {
        return block.defaultBlockState();
    }
    public int getWeight() {
        return weight;
    }

    public void placeAt(@NotNull LevelAccessor level, BlockPos pos) {
        level.setBlock(pos, getState(), 3);
    }

    public Block getBlock() {
        return block;
    }
}
