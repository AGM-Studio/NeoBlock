package xyz.agmstudio.neoblock.neo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.platform.IConfig;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NeoChestSpec extends NeoBlockSpec {
    private static final Pattern PATTERN = Pattern.compile("^(?:(?<count>\\d+)x *)?neoblock:chest:(?<id>[^ ]+)$");

    private static final HashMap<String, Holder> CHESTS = new HashMap<>();

    public static void reloadChests() {
        IConfig config = IConfig.getConfig(NeoBlock.CONFIG_FOLDER, "chests");
        if (config == null) {
            NeoBlock.LOGGER.error("Failed to load chests config.");
            return;
        }

        CHESTS.clear();

        config.forEach((key, value) -> {
            IConfig section = config.getSection(key);

            List<NeoItemSpec> list = new ArrayList<>();
            List<String> items = section.get("items", List.of());
            items.forEach(entry -> NeoItemSpec.parseItem(entry).ifPresent(list::add));
            if (list.isEmpty()) NeoBlock.LOGGER.info("Unable to load chest {} because it's empty.", key);
            else {
                NeoBlock.LOGGER.info("Loaded {} stacks for {}", list.size(), key);
                int min = Math.max(0, section.getInt("min", 0));
                int max = Math.min(27, section.getInt("max", 27));
                CHESTS.put(key, new Holder(list, min, max));
            }
        });

        NeoBlock.LOGGER.info("Loaded {} chests.", CHESTS.size());
    }

    public static Optional<NeoChestSpec> parseChest(String input) {
        Matcher matcher = PATTERN.matcher(input.trim());
        if (!matcher.matches()) return Optional.empty();

        String name = matcher.group("id");
        Holder chest = CHESTS.getOrDefault(name, null);
        if (chest == null) NeoBlock.LOGGER.warn("Unknown chest ID: '{}'", name);

        String countString = matcher.group("count");
        int count = (countString != null) ? Integer.parseInt(countString) : 1;
        return Optional.of(new NeoChestSpec(chest, count, name));
    }

    private final Holder holder;
    private final String id;

    public NeoChestSpec(Holder holder, int weight, String id) {
        super(Blocks.CHEST, weight);

        this.holder = holder;
        this.id = id;
    }

    @Override public String getID() {
        String range = weight > 1 ? weight + "x " : "";
        return range + "neoblock:chest:" + id;
    }

    @Override public void placeAt(@NotNull LevelAccessor level, BlockPos pos) {
        super.placeAt(level, pos);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            List<Integer> slots = IntStream.range(0, chest.getContainerSize()).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            int count = 0;
            int roll = 0;
            do {
                for (NeoItemSpec item : this.holder.items) {
                    ItemStack result = item.getStackWithChance();
                    if (result == null || count >= holder.max) continue;

                    int slot = slots.remove(0);
                    chest.setItem(slot, result);
                    count += 1;
                }

                roll += 1;
            } while (count < holder.min || roll < 20);
        }
    }

    @Override public NeoBlockSpec copy() {
        return copy(weight);
    }
    @Override public NeoBlockSpec copy(int weight) {
        return new NeoChestSpec(holder, weight, id);
    }

    @Override public String toString() {
        return getID() + "{" + holder.items.stream().map(Object::toString).collect(Collectors.joining(";")) + "}";
    }

    public record Holder(List<NeoItemSpec> items, int min, int max) {}
}
