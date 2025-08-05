package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.minecraft.ItemAPI;
import xyz.agmstudio.neoblock.minecraft.MessengerAPI;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoMobStack extends NeoItemStack {
    private static final Pattern MOB_PATTERN = Pattern.compile("mob:(?<count>\\d+(-\\d+)?)?x?(?<id>[\\w:]+)(?:\\s+(?<chance>\\d+\\.?\\d*)%?)?");
    @NotNull private static final ResourceLocation DEFAULT = MinecraftAPI.parseResourceLocation("minecraft:pig");
    @NotNull private static final EntityType<?> DEFAULT_MOB = MinecraftAPI.getEntityType(DEFAULT).get();
    @NotNull private static final Item DEFAULT_EGG = MinecraftAPI.getItem("minecraft:egg").get();

    private final EntityType<?> mob;

    public NeoMobStack(EntityType<?> mob, UniformInt range, double chance, Item egg) {
        super(egg, range, chance);
        this.mob = mob;
    }

    public EntityType<?> getMob() {
        return mob;
    }

    @Override public ItemStack modify(ItemStack item) {
        CompoundTag tag = ItemAPI.getItemTag(item);

        @Nullable ResourceLocation location = MinecraftAPI.getEntityTypeResource(mob).orElse(null);
        tag.putBoolean("isNeoMob", true);
        tag.putString("neoMobType", location != null ? location.toString() : DEFAULT.toString());

        ItemAPI.setItemTag(item, tag);
        return item;
    }

    @Override public ResourceLocation getResource() {
        return MinecraftAPI.getEntityTypeResource(mob).orElse(DEFAULT);
    }
    @Override
    public String getId() {
        return "mob:" + getResource();
    }

    public static Optional<NeoMobStack> parseMob(String input) {
        if (input == null) return Optional.empty();

        Matcher matcher = MOB_PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        String id = matcher.group("id");
        EntityType<?> entityType = EntityType.byString(id).orElse(null);
        if (entityType == null) return Optional.empty();

        Item spawnEgg = MinecraftAPI.getItem(id + "_spawn_egg").orElse(DEFAULT_EGG);
        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));

        return Optional.of(new NeoMobStack(entityType, range, chance, spawnEgg));
    }

    public static Optional<EntityType<?>> getMobTradeEntity(ItemStack item) {
        if (item == null) return Optional.empty();

        CompoundTag tag = ItemAPI.getItemTag(item);
        if (!tag.getBoolean("isNeoMob")) return Optional.empty();
        return MinecraftAPI.getEntityType(tag.getString("neoMobType"));
    }

    public static boolean handlePossibleMobTrade(ItemStack item, ServerLevel level) {
        Optional<EntityType<?>> mob = getMobTradeEntity(item);
        if (mob.isEmpty()) return false;

        MessengerAPI.sendInstantMessage("message.neoblock.trades.mob", level, true, item.getCount(), mob.get().getDescription());
        WorldData.addTradedMob(mob.get(), item.getCount());
        item.setCount(0);

        return true;
    }
}
