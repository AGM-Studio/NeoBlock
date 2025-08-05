package xyz.agmstudio.neoblock.neo.loot.chest;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.loot.NeoBlockSpec;
import xyz.agmstudio.neoblock.neo.loot.NeoItemStack;
import xyz.agmstudio.neoblock.util.ResourceUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeoChestSpec extends NeoBlockSpec {
    private static final Pattern CHEST_PATTERN = Pattern.compile("^(?:(?<count>\\d+)x)?neoblock:(?<id>[^ ]+)$");

    private static final Path FOLDER = MinecraftAPI.CONFIG_DIR.resolve(NeoBlockMod.MOD_ID);
    private static final HashMap<String, List<NeoItemStack>> CHESTS = new HashMap<>();

    public static List<NeoItemStack> getChestItems(String name) {
        return CHESTS.getOrDefault(name, List.of());
    }

    public static void reloadChests() {
        CommentedFileConfig config = ResourceUtil.getConfig(FOLDER, "chests");
        if (config == null) {
            NeoBlockMod.LOGGER.error("Failed to load chests config.");
            return;
        }

        CHESTS.clear();

        for (String key : config.valueMap().keySet()) {
            Set<String> visited = new HashSet<>();
            List<String> entries = config.get(key);
            if (entries == null) {
                CHESTS.put(key, List.of());
                NeoBlockMod.LOGGER.info("Loaded 0 stacks for {}", key);
                break;
            }

            List<NeoItemStack> list = new ArrayList<>();
            entries.forEach(entry -> NeoItemStack.parseItem(entry).ifPresent(list::add));

            CHESTS.put(key, list);
            NeoBlockMod.LOGGER.info("Loaded {} stacks for {}", list.size(), key);
        }

        NeoBlockMod.LOGGER.info("Loaded {} chests.", CHESTS.size());
    }

    public static Optional<NeoChestSpec> parseChest(String input) {
        Matcher matcher = CHEST_PATTERN.matcher(input.trim());
        if (!matcher.matches()) return Optional.empty();

        List<NeoItemStack> items = CHESTS.getOrDefault(matcher.group("id"), List.of());
        if (items.isEmpty()) NeoBlockMod.LOGGER.warn("Unknown chest ID: '{}'", matcher.group("id"));

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new NeoChestSpec(items, count));
    }

    private final List<NeoItemStack> items;

    public NeoChestSpec(List<NeoItemStack> items, int weight) {
        super(Blocks.CHEST, weight);

        this.items = items;
    }

    @Override public void placeAt(@NotNull LevelAccessor level, BlockPos pos) {
        super.placeAt(level, pos);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            List<Integer> slots = IntStream.range(0, chest.getContainerSize()).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            for (NeoItemStack item: items) {
                ItemStack result = item.getStackWithChance();
                if (result == null) continue;

                int slot = slots.removeFirst();
                chest.setItem(slot, result);
            }
        }
    }
}
