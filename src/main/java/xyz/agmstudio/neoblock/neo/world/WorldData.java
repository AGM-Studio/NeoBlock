package xyz.agmstudio.neoblock.neo.world;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.data.Schematic;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.neo.block.BlockManager;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class WorldData extends MinecraftAPI.AbstractWorldData {
    private static WorldData instance;
    public static WorldData getInstance() {
        return instance;
    }

    public static void resetTiers() {
        resetTiers(instance);
    }
    public static void resetTiers(WorldData data) {
        data.tiers.clear();
        TierData.stream().map(tier -> WorldTier.of(tier, data)).forEach(data.tiers::add);

        instance.status.hash = TierData.getHash();
    }

    public static void setup(@NotNull ServerLevel level) {
        load(level);

        if (instance == null) return;
        if (instance.status.state == WorldStatus.State.INACTIVE) {
            boolean isNeoBlock = true;
            for (int y : List.of(-64, -61, 0, 64))
                if (!level.getBlockState(new BlockPos(0, y, 0)).isAir()) isNeoBlock = false;

            if (isNeoBlock) {
                BlockManager.DEFAULT_SPEC.placeAt(level, BlockManager.POS);
                UnmodifiableConfig rules = NeoBlockMod.getConfig().get("rules");
                if (rules != null) WorldRules.applyGameRules(level, rules);

                // Load schematics from config!
                Schematic.loadSchematic(level, BlockManager.POS, "main.nbt");
                int iterator = 0;
                while (NeoBlockMod.getConfig().contains("schematics.custom_" + iterator)) {
                    try {
                        UnmodifiableConfig scheme = NeoBlockMod.getConfig().get("schematics.custom_" + iterator);
                        String name = scheme.getOrElse("name", "NeoBlockSchematic_" + iterator);
                        BlockPos pos = new BlockPos(scheme.getInt("x"), scheme.getInt("y"), scheme.getInt("z"));
                        int result = Schematic.loadSchematic(level, pos, name);
                        if (result == 0) throw new FileNotFoundException("File \"" + name + "\" not found");
                    } catch (Exception e) {
                        NeoBlockMod.LOGGER.error("Unable to load schematic {}", iterator, e);
                    }
                    iterator++;
                }

                instance.status.state = WorldStatus.State.ACTIVE;
                instance.setDirty();
            } else {
                NeoBlockMod.LOGGER.info("NeoBlock has been disabled.");
                MessengerAPI.sendMessage("message.neoblock.disabled_world_1", level, false);
                MessengerAPI.sendMessage("message.neoblock.disabled_world_2", level, false);

                instance.status.state = WorldStatus.State.DISABLED;
                instance.setDirty();
            }
        } else if (instance.status.state == WorldStatus.State.UPDATED) {
            NeoBlockMod.LOGGER.info("NeoBlock tiers has been updated.");
            Component command = Component.literal("/neoblock force reset").withStyle(
                    Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/neoblock force reset")).withColor(ChatFormatting.AQUA)
            );
            MessengerAPI.sendMessage("message.neoblock.updated_world", level, false, command);

            instance.status.state = WorldStatus.State.UPDATED;
            instance.setDirty();
        }
    }

    public static @NotNull WorldData create(@NotNull ServerLevel level) {
        WorldData data = new WorldData(level);

        data.status = new WorldStatus(data);
        resetTiers(data);

        NeoBlockMod.LOGGER.debug("Creating new world data");
        return data;
    }
    public static @NotNull WorldData load(@NotNull CompoundTag tag, ServerLevel level) {
        WorldData data = new WorldData(level);

        NeoBlockMod.LOGGER.debug("Loading WorldData from {}", tag);
        data.status = NBTSaveable.load(WorldStatus.class, tag, data);
        if (!Objects.equals(instance.status.hash, TierData.getHash())) {
            data.status.setUpdated();
            return data;
        }

        final ListTag tiers = tag.getList("Tiers", StringTag.TAG_COMPOUND);
        for (int i = 0; i < tiers.size(); i++) {
            WorldTier tier = NBTSaveable.load(WorldTier.class, tiers.getCompound(i));
            if (tier.canBeUnlocked()) data.upgrade.addUpgrade(tier);
            data.tiers.add(tier);
        }

        return data;
    }

    @Override public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.merge(status.save());
        if (status.isActive()) {
            ListTag list = new ListTag();
            for (WorldTier tier: tiers) list.add(tier.save());
            tag.put("Tiers", list);
        }

        NeoBlockMod.LOGGER.debug("WorldData saved as {}", tag);
        return tag;
    }

    private final ServerLevel level;

    private WorldStatus status;
    private final HashSet<WorldTier> tiers = new HashSet<>();
    private final WorldUpgrade upgrade = new WorldUpgrade();

    private WorldData(ServerLevel level) {
        instance = this;
        this.level = level;
    }

    public static @NotNull RandomSource getRandom() {
        return instance.level.getRandom();
    }
    public WorldStatus getStatus() {
        return status;
    }
    public static WorldStatus getWorldStatus() {
        return instance.status;
    }
    public ServerLevel getLevel() {
        return level;
    }
    public static ServerLevel getWorldLevel() {
        return instance.level;
    }
    public WorldTier getTier(int id) {
        for (WorldTier tier: tiers) if (tier.getID() == id) return tier;
        return null;
    }
    public static WorldTier getWorldTier(int id) {
        return instance.getTier(id);
    }

    public HashSet<WorldTier> getTiers() {
        return tiers;
    }
    public static HashSet<WorldTier> getWorldTiers() {
        return instance.tiers;
    }
    public static int totalWeight() {
        return instance.tiers.stream().filter(WorldTier::isEnabled).mapToInt(WorldTier::getWeight).sum();
    }

    public WorldUpgrade getUpgrade() {
        return upgrade;
    }
    public static WorldUpgrade getWorldUpgrade() {
        return instance.upgrade;
    }

    public static void setCommanded(WorldTier tier, boolean force) {
        tier.lock.commanded = true;

        if (force && tier.canBeUnlocked()) instance.upgrade.addUpgrade(tier);
    }

    public static void tick(ServerLevel level, LevelAccessor access) {
        if (instance != null) instance.upgrade.tick(level, access);
    }
}
