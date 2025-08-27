package xyz.agmstudio.neoblock.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.helpers.IMinecraftHelper;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.Optional;
import java.util.function.BiFunction;

public final class ForgeMinecraftHelper implements IMinecraftHelper {
    @Override public @NotNull ResourceLocation parseResourceLocation(String name) {
        return new ResourceLocation(name);
    }
    @Override public Optional<ResourceLocation> getResourceLocation(String name) {
        return Optional.ofNullable(ResourceLocation.tryParse(name));
    }
    @Override public ResourceLocation createResourceLocation(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    @Override public Optional<Item> getItem(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(location));
    }
    @Override public Optional<ResourceLocation> getItemResource(Item item) {
        if (item == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.ITEMS.getKey(item));
    }

    @Override public Optional<Block> getBlock(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(location));
    }
    @Override public Optional<ResourceLocation> getBlockResource(Block block) {
        if (block == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getKey(block));
    }

    @Override public Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        if (location == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(location));
    }
    @Override public Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        if (type == null) return Optional.empty();
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    @Override public Optional<MobEffect> getMobEffect(ResourceLocation location) {
        return Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getValue(location));
    }
    @Override public Optional<ResourceLocation> getMobEffectResource(MobEffect effect) {
        return Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getKey(effect));
    }

    @Override public BiFunction<MobEffect, Integer, MobEffectInstance> effectFactory() {
        return MobEffectInstance::new;
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

    private static ItemStack toItemCost(NeoItemSpec item, RandomSource random) {
        return new ItemStack(item.getItem(), item.getRange().sample(random));
    }

    @Override public Optional<MerchantOffer> getOfferOf(NeoItemSpec result, NeoItemSpec costA, NeoItemSpec costB, UniformInt uses) {
        @NotNull final RandomSource RNG = WorldData.getRandom();
        @NotNull final Item AIR = net.minecraft.world.item.Items.AIR;

        ItemStack r = toItemStack(result, RNG);
        ItemStack a = toItemCost(costA, RNG);
        ItemStack b = costB != null ? toItemCost(costB, RNG) : ItemStack.EMPTY;

        if (r.getItem() == AIR || a.getItem() == AIR) return Optional.empty();
        return Optional.of(new MerchantOffer(a, b, r, uses.sample(RNG), 0, 0));
    }

    @Override public Objective createScoreboardObjective(Scoreboard scoreboard, String name, ObjectiveCriteria criteria, String title, ObjectiveCriteria.RenderType renderType) {
        return scoreboard.addObjective(name, criteria, Component.translatable(title), renderType);
    }
    @Override public void setScoreboardDisplay(Scoreboard scoreboard, MinecraftUtil.ScoreboardSlots slot, Objective objective) {
        scoreboard.setDisplayObjective(slot.id, objective);
    }

    private Score capturePlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
    }
    @Override public void setPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        capturePlayerScore(scoreboard, player, objective).setScore(amount);
    }
    @Override public void addPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        capturePlayerScore(scoreboard, player, objective).add(amount);
    }
    @Override public int getPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return capturePlayerScore(scoreboard, player, objective).getScore();
    }
}
