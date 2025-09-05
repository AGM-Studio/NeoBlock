package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTagBlockSpec extends NeoBlockSpec {
    private static final Pattern PATTERN = Pattern.compile("^(?:(?<count>\\d+)x *)?#(?<id>[a-z0-9_]+:[a-z0-9_/]+)$");
    private final Supplier<List<Block>> supplier;
    private final ResourceLocation location;

    public NeoTagBlockSpec(TagKey<Block> tag, int weight) {
        super(Blocks.AIR, weight);
        this.location = tag.location();
        this.supplier = () -> MinecraftUtil.getBlocksOfTag(tag);
    }

    public NeoTagBlockSpec(List<Block> list, String name, int weight) {
        super(Blocks.AIR, weight);
        this.location = MinecraftUtil.createResourceLocation(NeoBlock.MOD_ID, name);
        this.supplier = () -> new ArrayList<>(list);
    }

    @Override public Block getBlock() {
        List<Block> blocks = supplier.get();
        if (blocks.isEmpty()) {
            NeoBlock.LOGGER.warn("No item for #{} was found!", location);
            return super.getBlock();
        }
        return blocks.get(WorldData.getRandom().nextInt(blocks.size()));
    }

    @Override public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + "#" + location;
    }

    public static Optional<NeoTagBlockSpec> parseTagBlock(String input) {
        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        ResourceLocation tagLoc = MinecraftUtil.parseResourceLocation(matcher.group("id"));
        TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagLoc);

        return Optional.of(new NeoTagBlockSpec(tag, matcher.end("count")));
    }
}
