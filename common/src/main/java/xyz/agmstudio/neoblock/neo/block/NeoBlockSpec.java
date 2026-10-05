package xyz.agmstudio.neoblock.neo.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.NeoNBT;
import xyz.agmstudio.neocore.util.StringUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoBlockSpec {
    private static final Pattern PATTERN = StringUtil.COUNT.optional()
            .then(StringUtil.namespace("block"))
            .then(StringUtil.BLOCK_STATE.optional())
            .then(StringUtil.BLOCK_NBT.optional()).build(true);
    protected static Block getDefault() {
        return NeoBlock.DEFAULT_SPEC.getBlock();
    }

    protected final Block block;
    protected final BlockState state;
    protected final CompoundTag nbt;
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
            NeoBlockMod.getLogger().warn("Invalid block: '{}'", input);
            return Optional.empty();
        }

        String blockString = matcher.group("block");
        Optional<Block> blockOpt = NeoMC.getBlock(blockString);
        if (blockOpt.isEmpty()) {
            NeoBlockMod.getLogger().warn("Unknown block ID: '{}'", blockString);
            return Optional.empty();
        }

        Block block = blockOpt.get();
        BlockState resolvedState = block.defaultBlockState();

        String stateGroup = matcher.group("states");
        if (stateGroup != null && stateGroup.length() > 2) {
            String propsContent = stateGroup.substring(1, stateGroup.length() - 1);
            resolvedState = applyProperties(resolvedState, propsContent);
        }

        String nbtGroup = matcher.group("nbt");
        CompoundTag parsedNbt = null;
        if (nbtGroup != null && !nbtGroup.isBlank()) {
            try {
                parsedNbt = TagParser.parseTag(nbtGroup);
            } catch (CommandSyntaxException e) {
                NeoBlockMod.getLogger().warn("Invalid block NBT '{}': {}", nbtGroup, e.getMessage());
            }
        }

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;

        return Optional.of(new NeoBlockSpec(block, resolvedState, parsedNbt, count));
    }

    private static BlockState applyProperties(BlockState state, @NotNull String propertiesString) {
        for (String entry : propertiesString.split(",")) {
            String[] kv = entry.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();

            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property != null) state = setValueHelper(state, property, val);
            else NeoBlockMod.getLogger().warn("Property '{}' not found on block '{}'", key, state.getBlock());
        }
        return state;
    }

    private static <T extends Comparable<T>> @NotNull BlockState setValueHelper(BlockState state, @NotNull Property<T> property, String valueStr) {
        Optional<T> value = property.getValue(valueStr);
        if (value.isPresent()) return state.setValue(property, value.get());
        NeoBlockMod.getLogger().warn("Invalid value '{}' for property '{}' on block '{}'", valueStr, property.getName(), state.getBlock());
        return state;
    }

    public NeoBlockSpec(Block block, int weight) {
        this.block = block;
        this.weight = weight;
        this.state = null;
        this.nbt = null;
    }

    public NeoBlockSpec(Block block, BlockState state, @Nullable CompoundTag nbt, int weight) {
        this.block = block;
        this.state = state;
        this.weight = weight;
        this.nbt = nbt;
    }

    public NeoBlockSpec(Block block) {
        this(block, 1);
    }

    public Block getBlock() {
        return block;
    }
    public BlockState getState() {
        return state == null ? getBlock().defaultBlockState() : state;
    }
    public @Nullable CompoundTag getNbt() {
        return nbt;
    }
    public int getWeight() {
        return weight;
    }
    public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + NeoMC.getBlockResource(getBlock()).orElse(null);
    }

    public void placeAt(@NotNull NeoBlock block) {
        BlockPos pos = block.getBlockPos();
        block.level.setBlock(pos, getState(), 3);
        block.ensureNoFall();

        if (this.nbt != null) {
            BlockEntity be = block.level.getBlockEntity(pos);
            if (be != null) NeoNBT.Block.loadBlockEntity(be, this.nbt, block.level);
        }
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