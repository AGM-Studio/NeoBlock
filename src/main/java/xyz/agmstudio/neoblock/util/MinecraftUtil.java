package xyz.agmstudio.neoblock.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.tiers.merchants.NeoItem;

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
    public static final IEventBus EVENT_BUS = MinecraftForge.EVENT_BUS;

    public static @Nullable ResourceLocation getResourceLocation(String name) {
        return ResourceLocation.tryParse(name);
    }
    public static @Nullable Item getItem(String name) {
        return getItem(getResourceLocation(name));
    }
    public static @Nullable Item getItem(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.ITEMS.getValue(location);
    }
    public static @Nullable ResourceLocation getItemResource(Item item) {
        if (item == null) return null;
        return ForgeRegistries.ITEMS.getKey(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return item != null && getItemResource(item) == location;
    }
    public static @Nullable Block getBlock(String name) {
        return getBlock(getResourceLocation(name));
    }
    public static @Nullable Block getBlock(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.BLOCKS.getValue(location);
    }
    public static @Nullable ResourceLocation getBlockResource(Block block) {
        if (block == null) return null;
        return ForgeRegistries.BLOCKS.getKey(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return block != null && getBlockResource(block) == location;
    }
    public static @Nullable BlockState getBlockState(String name) {
        return getBlockState(getResourceLocation(name));
    }
    public static @Nullable BlockState getBlockState(ResourceLocation location) {
        if (location == null) return null;
        Block block = getBlock(location);
        return block != null ? block.defaultBlockState() : null;
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return state != null && getBlockResource(state.getBlock()) == location;
    }
    public static @Nullable EntityType<?> getEntityType(String name) {
        return getEntityType(getResourceLocation(name));
    }
    public static @Nullable EntityType<?> getEntityType(ResourceLocation location) {
        if (location == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(location);
    }
    public static @Nullable ResourceLocation getEntityTypeResource(EntityType<?> type) {
        if (type == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getKey(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return entityType != null && getEntityTypeResource(entityType) == location;
    }

    public static final class Items {
        public static @NotNull CompoundTag getItemTag(ItemStack item) {
            return item.getOrCreateTag();
        }

        public static void setItemTag(@NotNull ItemStack item, CompoundTag tag) {
            item.setTag(tag);
        }
    }

    public static final class Entities {
        public static void leash(Entity mob, Entity to) {
            if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
        }
    }

    public static final class Merchant {
        public static MerchantOffer getOfferOf(NeoItem costA, NeoItem costB, NeoItem result, Range uses) {
            ItemStack r = new ItemStack(result.getItem(), result.getCount().get());
            ItemStack a = new ItemStack(costA.getItem(), costA.getCount().get());
            ItemStack b = costB != null ? new ItemStack(costB.getItem(), costB.getCount().get()) : null;

            if (b != null) return new MerchantOffer(a, b, r, uses.get(), 0, 0);
            return new MerchantOffer(a, r, uses.get(), 0, 0);
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
                    (tag) -> WorldData.load(tag, level),
                    () -> WorldData.create(level),
                    DATA_NAME);
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
                return NbtIo.readCompressed(is);
            }
        }

        public static Tag writeBlockPos(BlockPos pos) {
            return NbtUtils.writeBlockPos(pos);
        }
        public static Tag writeBlockState(BlockState state) {
            return NbtUtils.writeBlockState(state);
        }

        public static BlockPos readBlockPos(CompoundTag tag, String key, BlockPos def) {
            return NbtUtils.readBlockPos(tag.getCompound(key));
        }
        public static BlockState readBlockState(CompoundTag tag, String key, ServerLevel level) {
            return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound(key));
        }

        public static CompoundTag getBlockEntity(BlockEntity blockEntity, ServerLevel level) {
            if (blockEntity == null) return null;
            return blockEntity.saveWithFullMetadata();
        }
        public static void loadBlockEntity(BlockEntity be, CompoundTag tag, ServerLevel level) {
            if (be == null || tag == null) return;
            be.load(tag);
            be.setChanged();
        }
    }
}
