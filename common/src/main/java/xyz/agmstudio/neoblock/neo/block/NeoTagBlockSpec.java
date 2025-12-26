package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neoblock.platform.IConfig;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.PatternUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoTagBlockSpec extends NeoBlockSpec {
    private static final Pattern PATTERN =
            PatternUtil.COUNT.optional().then("#").then(PatternUtil.NAMESPACE).build(true);
    private static final HashMap<String, List<NeoBlockSpec>> MAP = new HashMap<>();

    public static void reloadTags() {
        IConfig config = IConfig.getConfig(NeoBlock.CONFIG_FOLDER, "tags");
        IConfig section = config != null ? config.getSection("blocks") : null;
        if (section == null) {
            NeoBlock.LOGGER.error("Failed to load block tags from configs.");
            return;
        }

        section.forEach((key, value) -> {
            List<String> list = section.get(key);
            if (list == null || list.isEmpty()) {
                NeoBlock.LOGGER.warn("Failed to load blocks from blocks.{}.", key);
                return;
            }

            List<NeoBlockSpec> result = new ArrayList<>();
            list.forEach(item -> NeoBlockSpec.parse(item).ifPresent(result::add));

            MAP.put(key, result);
            NeoBlock.LOGGER.info("Loaded {} blocks for tag #neoblock:{}.", list.size(), key);
        });
    }

    private final Supplier<Block> supplier;
    private final ResourceLocation location;

    public NeoTagBlockSpec(TagKey<Block> tag, int weight) {
        super(Blocks.AIR, weight);
        this.location = tag.location();
        this.supplier = () -> this.ofTag(tag);
    }
    private Block ofTag(TagKey<Block> tag) {
        List<Block> blocks = MinecraftUtil.getBlocksOfTag(tag);
        Optional<Block> block = WorldManager.getRandomItem(blocks);
        if (block.isEmpty()) {
            NeoBlock.LOGGER.warn("Tag key {} has no items to choose from.", location);
            return NeoBlockSpec.getDefault();
        }
        return block.get();
    }

    public NeoTagBlockSpec(String name, int weight) {
        super(Blocks.AIR, weight);
        this.location = MinecraftUtil.createResourceLocation(NeoBlock.MOD_ID, name);

        List<NeoBlockSpec> list = MAP.getOrDefault(name, List.of());
        this.supplier = () -> this.ofList(list);
    }
    private Block ofList(List<NeoBlockSpec> list) {
        Optional<NeoBlockSpec> block = WorldManager.getRandomItem(list);
        if (block.isEmpty()) {
            NeoBlock.LOGGER.warn("Custom list {} has no items to choose from.", list);
            return NeoBlockSpec.getDefault();
        }
        return block.get().getBlock();
    }

    @Override public Block getBlock() {
        return supplier.get();
    }

    @Override public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + "#" + location;
    }

    public static Optional<NeoTagBlockSpec> parseTagBlock(String input) {
        Matcher matcher = PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;

        ResourceLocation location = MinecraftUtil.parseResourceLocation(matcher.group("id"));
        if (location.getNamespace().equals(NeoBlock.MOD_ID)) {
            if (MAP.getOrDefault(location.getPath(), List.of()).isEmpty()) {
                NeoBlock.LOGGER.warn("Tag block #{} is empty.", location);
                return Optional.empty();
            }

            return Optional.of(new NeoTagBlockSpec(location.getPath(), count));
        }

        TagKey<Block> tag = TagKey.create(Registries.BLOCK, location);

        return Optional.of(new NeoTagBlockSpec(tag, count));
    }
}
