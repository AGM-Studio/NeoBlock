package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.helpers.IMinecraftHelper;

import java.util.Optional;
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
}
