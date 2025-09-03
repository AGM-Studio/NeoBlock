package xyz.agmstudio.neoblock.platform;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public final class NeoForgeMinecraftHelper implements IMinecraftHelper {
    @Override public @NotNull ResourceLocation parseResourceLocation(String name) {
        return ResourceLocation.parse(name);
    }
    @Override public Optional<ResourceLocation> getResourceLocation(String name) {
        return Optional.ofNullable(ResourceLocation.tryParse(name));
    }
    @Override public ResourceLocation createResourceLocation(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override public Optional<Item> getItem(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.get(location));
    }
    @Override public Optional<ResourceLocation> getItemResource(Item item) {
        if (item == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ITEM.getKey(item));
    }

    @Override public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return stack.getEnchantmentLevel(Holder.direct(enchantment));
    }
    @Override public int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.has(DataComponents.ENCHANTMENTS)) {
            ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
            if (enchants != null) {
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet())
                    if (entry.getKey().is(enchantment)) return entry.getIntValue();
            }
        }

        return 0;
    }
    @Override public boolean isSilkTouched(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.SILK_TOUCH) > 0;
    }

    @Override public boolean canBreak(Item tool, BlockState block) {
        Tool component = tool.components().get(DataComponents.TOOL);
        if (component == null) return false;

        return component.isCorrectForDrops(block);
    }

    @Override public Optional<Block> getBlock(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.get(location));
    }
    @Override public Optional<ResourceLocation> getBlockResource(Block block) {
        if (block == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.BLOCK.getKey(block));
    }

    @Override public Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.get(location));
    }
    @Override public Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        if (type == null) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    @Override public <T extends Entity> T spawnEntity(ServerLevel level, EntityType<T> type, BlockPos pos) {
        return type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
    }
    @Override public void teleportEntity(Entity entity, ServerLevel level, double ox, double oy, double oz, int ry, int rx) {
        entity.teleportTo(level, ox, oy, oz, Set.of(), ry, rx);
    }

    @Override public Optional<MobEffect> getMobEffect(ResourceLocation location) {
        return Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.get(location));
    }
    @Override public Optional<ResourceLocation> getMobEffectResource(MobEffect effect) {
        return Optional.ofNullable(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }

    @Override public BiFunction<MobEffect, Integer, MobEffectInstance> effectFactory() {
        return (effect, time) -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), time);
    }

    @Override public Iterable<Entity> iterateEntities(ServerLevel level) {
        return level.getEntities().getAll();
    }

    @Override public void leash(Entity mob, Mob to) {
        if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
    }

    private static ItemStack toItemStack(NeoItemSpec item, RandomSource random) {
        ItemStack stack = new ItemStack(item.getItem(), item.getRange().sample(random));
        return item.modify(stack);
    }

    private static ItemCost toItemCost(NeoItemSpec item, RandomSource random) {
        return new ItemCost(item.getItem(), item.getRange().sample(random));
    }

    @Override public Optional<MerchantOffer> getOfferOf(NeoItemSpec result, NeoItemSpec costA, NeoItemSpec costB, UniformInt uses) {
        @NotNull final RandomSource RNG = WorldData.getRandom();
        @NotNull final Item AIR = net.minecraft.world.item.Items.AIR;

        ItemStack r = toItemStack(result, RNG);
        ItemCost a = toItemCost(costA, RNG);
        Optional<ItemCost> b = costB != null ? Optional.of(toItemCost(costB, RNG)) : Optional.empty();

        if (r.getItem() == AIR || a.itemStack().getItem() == AIR) return Optional.empty();
        return Optional.of(new MerchantOffer(a, b, r, uses.sample(RNG), 0, 0));
    }

    @Override public Objective createScoreboardObjective(Scoreboard scoreboard, String name, ObjectiveCriteria criteria, String title, ObjectiveCriteria.RenderType renderType) {
        return scoreboard.addObjective(name, criteria, Component.translatable(title), renderType, true, null);
    }
    @Override public void setScoreboardDisplay(Scoreboard scoreboard, MinecraftUtil.ScoreboardSlots slot, Objective objective) {
        DisplaySlot convert = switch (slot) {
            case LIST -> DisplaySlot.LIST;
            case SIDEBAR ->  DisplaySlot.SIDEBAR;
            case BELOW_NAME ->  DisplaySlot.BELOW_NAME;
            case TEAM_BLACK -> DisplaySlot.TEAM_BLACK;
            case TEAM_DARK_BLUE -> DisplaySlot.TEAM_DARK_BLUE;
            case TEAM_DARK_GREEN -> DisplaySlot.TEAM_DARK_GREEN;
            case TEAM_DARK_AQUA -> DisplaySlot.TEAM_DARK_AQUA;
            case TEAM_DARK_RED -> DisplaySlot.TEAM_DARK_RED;
            case TEAM_DARK_PURPLE -> DisplaySlot.TEAM_DARK_PURPLE;
            case TEAM_GOLD -> DisplaySlot.TEAM_GOLD;
            case TEAM_GRAY -> DisplaySlot.TEAM_GRAY;
            case TEAM_DARK_GRAY -> DisplaySlot.TEAM_DARK_GRAY;
            case TEAM_BLUE -> DisplaySlot.TEAM_BLUE;
            case TEAM_GREEN -> DisplaySlot.TEAM_GREEN;
            case TEAM_AQUA -> DisplaySlot.TEAM_AQUA;
            case TEAM_RED -> DisplaySlot.TEAM_RED;
            case TEAM_LIGHT_PURPLE -> DisplaySlot.TEAM_LIGHT_PURPLE;
            case TEAM_YELLOW -> DisplaySlot.TEAM_YELLOW;
            case TEAM_WHITE -> DisplaySlot.TEAM_WHITE;
        };

        scoreboard.setDisplayObjective(convert, objective);
    }

    private ScoreAccess capturePlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return scoreboard.getOrCreatePlayerScore(ScoreHolder.fromGameProfile(player.getGameProfile()), objective);
    }
    @Override public void setPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        capturePlayerScore(scoreboard, player, objective).set(amount);
    }
    @Override public void addPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        capturePlayerScore(scoreboard, player, objective).add(amount);
    }
    @Override public int getPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return capturePlayerScore(scoreboard, player, objective).get();
    }

    @Override public DustParticleOptions getDustParticle(Vector3f color, float value) {
        return new DustParticleOptions(color, value);
    }

    @Override public int getLevelMinY(ServerLevel level) {
        return level.getMinBuildHeight();
    }
}
