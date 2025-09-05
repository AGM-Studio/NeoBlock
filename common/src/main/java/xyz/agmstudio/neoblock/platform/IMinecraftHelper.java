package xyz.agmstudio.neoblock.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface IMinecraftHelper {
    @NotNull ResourceLocation parseResourceLocation(String name);
    Optional<ResourceLocation> getResourceLocation(String name);
    ResourceLocation createResourceLocation(String namespace, String path);

    Optional<Item> getItem(ResourceLocation location);
    Optional<ResourceLocation> getItemResource(Item item);

    List<Item> getItemsOfTag(TagKey<Item> tag);

    int getEnchantmentLevel(ItemStack stack, Enchantment enchantment);
    int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment);
    boolean isSilkTouched(ItemStack stack);

    boolean canBreak(Item tool, BlockState block);

    Optional<Block> getBlock(ResourceLocation location);
    Optional<ResourceLocation> getBlockResource(Block block);

    List<Block> getBlocksOfTag(TagKey<Block> tag);

    Optional<EntityType<?>> getEntityType(ResourceLocation location);
    Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type);
    Iterable<ResourceLocation> getAllEntityTypes();

    <T extends Entity> T spawnEntity(ServerLevel level, EntityType<T> type, BlockPos pos);
    void teleportEntity(Entity entity, ServerLevel level, double ox, double oy, double oz, int ry, int rx);

    Optional<MobEffect> getMobEffect(ResourceLocation location);
    Optional<ResourceLocation> getMobEffectResource(MobEffect effect);

    BiFunction<MobEffect, Integer, MobEffectInstance> effectFactory();

    Iterable<Entity> iterateEntities(ServerLevel level);

    void leash(Entity mob, Mob to);

    Optional<MerchantOffer> getOfferOf(NeoItemSpec result, NeoItemSpec costA, NeoItemSpec costB, UniformInt uses);

    Objective createScoreboardObjective(Scoreboard scoreboard, String name, ObjectiveCriteria criteria, String title, ObjectiveCriteria.RenderType renderType);
    void setScoreboardDisplay(Scoreboard scoreboard, MinecraftUtil.ScoreboardSlots slot, Objective objective);
    void setPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount);
    void addPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount);
    int getPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective);

    DustParticleOptions getDustParticle(Vector3f color, float value);

    int getLevelMinY(ServerLevel level);
}
