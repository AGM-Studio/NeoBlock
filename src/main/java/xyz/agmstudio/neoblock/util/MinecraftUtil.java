package xyz.agmstudio.neoblock.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.loot.NeoItemStack;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftUtil {
    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    public static final IEventBus EVENT_BUS = NeoForge.EVENT_BUS;

    public static boolean isLoaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    public static @NotNull ResourceLocation parseResourceLocation(String name) {
        return ResourceLocation.parse(name);
    }
    public static Optional<ResourceLocation> getResourceLocation(String name) {
        return Optional.ofNullable(ResourceLocation.tryParse(name));
    }
    public static ResourceLocation createResourceLocation(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static Optional<Item> getItem(String name) {
        return getItem(getResourceLocation(name).get());
    }
    public static Optional<Item> getItem(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.get(location));
    }
    public static Optional<ResourceLocation> getItemResource(Item item) {
        if (item == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return getItemResource(item).get() == location;
    }
    public static Optional<Block> getBlock(String name) {
        return getBlock(getResourceLocation(name).get());
    }
    public static Optional<Block> getBlock(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.get(location));
    }
    public static Optional<ResourceLocation> getBlockResource(Block block) {
        if (block == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.getKey(block));
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return getBlockResource(block).get() == location;
    }
    public static Optional<BlockState> getBlockState(String name) {
        return getBlockState(getResourceLocation(name).get());
    }
    public static Optional<BlockState> getBlockState(ResourceLocation location) {
        return getBlock(location).map(Block::defaultBlockState);
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return getBlockResource(state.getBlock()).get() == location;
    }
    public static Optional<EntityType<?>> getEntityType(String name) {
        return getEntityType(getResourceLocation(name).get());
    }
    public static Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(location));
    }
    public static Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        if (type == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return getEntityTypeResource(entityType).get() == location;
    }

    public static final class Collection {
        public static <T> void shuffle(List<T> list) {
            @NotNull RandomSource rand = WorldData.getRandom();
            for (int i = list.size(); i > 1; i--) Collections.swap(list, i - 1, rand.nextInt(i));
        }
    }

    public static final class Items {
        public static @NotNull CompoundTag getItemTag(ItemStack item) {
            CustomData data = item.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            return data.copyTag();
        }

        public static void setItemTag(@NotNull ItemStack item, CompoundTag tag) {
            item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static final class Entities {
        public static void leash(Entity mob, Entity to) {
            if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
        }
    }

    public static final class Merchant {
        private static ItemStack toItemStack(NeoItemStack item, RandomSource random) {
            ItemStack stack = new ItemStack(item.getItem(), item.getRange().sample(random));
            return item.modify(stack);
        }
        private static ItemCost toItemCost(NeoItemStack item, RandomSource random) {
            return new ItemCost(item.getItem(), item.getRange().sample(random));
        }

        public static Optional<MerchantOffer> getOfferOf(NeoItemStack result, NeoItemStack costA, NeoItemStack costB, UniformInt uses) {
            @NotNull final RandomSource RNG = WorldData.getRandom();
            @NotNull final Item AIR = net.minecraft.world.item.Items.AIR;

            ItemStack r = toItemStack(result, RNG);
            ItemCost a = toItemCost(costA, RNG);
            Optional<ItemCost> b = costB != null ? Optional.of(toItemCost(costB, RNG)) : Optional.empty();

            if (r.getItem() == AIR || a.itemStack().getItem() == AIR) return Optional.empty();
            return Optional.of(new MerchantOffer(a, b, r, uses.sample(RNG), 0, 0));
        }
    }

    public static class Messenger {
        private static final HashMap<ServerLevel, List<MessageHolder>> messages = new HashMap<>();

        public static void sendMessage(String key, ServerLevel level, boolean action, Object... args) {
            sendMessage(Component.translatable(key, args), level, action);
        }
        public static void sendMessage(Component message, ServerLevel level, boolean action) {
            NeoBlockMod.LOGGER.info(message.getString());

            MessageHolder holder = new MessageHolder(message, action);
            for (Player player: level.players()) holder.send(player);

            messages.computeIfAbsent(level, k -> new ArrayList<>()).add(holder);
        }
        public static void sendInstantMessage(String key, Level level, boolean action, Object... args) {
            sendInstantMessage(Component.translatable(key, args), level, action);
        }
        public static void sendInstantMessage(Component message, Level level, boolean action) {
            NeoBlockMod.LOGGER.info(message.getString());

            MessageHolder holder = new MessageHolder(message, action);
            for (Player player: level.players()) holder.send(player);
        }

        public static void onPlayerJoin(ServerLevel level, Player player) {
            messages.getOrDefault(null, new ArrayList<>()).forEach(holder -> holder.send(player));
            messages.getOrDefault(level, new ArrayList<>()).forEach(holder -> holder.send(player));
        }

        static class MessageHolder {
            private final Set<Player> players = new HashSet<>();
            private final Component message;
            private final boolean action;

            protected MessageHolder(Component message, boolean action) {
                this.message = message;
                this.action = action;
            }

            public void send(Player player) {
                if (players.add(player)) player.displayClientMessage(message, action);
            }
        }
    }

    public static abstract class AbstractWorldData extends SavedData {
        private static final String DATA_NAME = "neo_block_data";
        public static @NotNull WorldData load(@NotNull ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new Factory<>(
                            () -> WorldData.create(level),
                            (tag, provider) -> WorldData.load(tag, level)
                    ), DATA_NAME);
        }

        public abstract @NotNull CompoundTag save(@NotNull CompoundTag tag);

        @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
            return save(tag);
        }
    }

    public static class MathUtil {
        public static long clamp(long min, long max, long value) {
            return Math.max(min, Math.min(max, value));
        }
        public static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
        public static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
        public static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public static class NBT {
        public static class IO {
            public static void write(Path file, CompoundTag nbt) throws IOException {
                OutputStream os = Files.newOutputStream(file);
                NbtIo.writeCompressed(nbt, os);
            }
            public static CompoundTag read(Path file) throws IOException {
                InputStream is = Files.newInputStream(file);
                return NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
            }
        }

        public static Tag writeBlockPos(BlockPos pos) {
            return NbtUtils.writeBlockPos(pos);
        }
        public static Tag writeBlockState(BlockState state) {
            return NbtUtils.writeBlockState(state);
        }

        public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
            return NbtUtils.readBlockPos(tag, key).orElse(def);
        }
        public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
            return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound(key));
        }

        public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
            if (blockEntity == null) return null;
            return blockEntity.saveWithFullMetadata(level.registryAccess());
        }
        public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
            if (be == null || tag == null) return;
            be.loadWithComponents(tag, level.registryAccess());
            be.setChanged();
        }
    }
}
