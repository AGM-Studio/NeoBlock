package xyz.agmstudio.neocore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import xyz.agmstudio.neocore.providers.ItemStackProvider;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class NeoMC {

    public static Path getMainConfigFolder() {
        return NeoMod.MC.getConfigFolder();
    }
    public static String getPlatformName() {
        return NeoMod.MC.getPlatformName();
    }
    public static boolean isModLoaded(String modId) {
        return NeoMod.MC.isModLoaded(modId);
    }
    public static boolean isDevelopmentEnvironment() {
        return NeoMod.MC.isDevelopmentEnvironment();
    }

    public static @NotNull ResourceLocation parseResourceLocation(String name) {
        return NeoMod.MC.parseResourceLocation(name);
    }
    public static Optional<ResourceLocation> getResourceLocation(String name) {
        return NeoMod.MC.getResourceLocation(name);
    }
    public static ResourceLocation createResourceLocation(String namespace, String path) {
        return NeoMod.MC.createResourceLocation(namespace, path);
    }

    public static Optional<Item> getItem(String name) {
        return getItem(getResourceLocation(name).orElse(null));
    }
    public static Optional<Item> getItem(ResourceLocation location) {
        return NeoMod.MC.getItem(location);
    }
    public static Optional<ResourceLocation> getItemResource(Item item) {
        return NeoMod.MC.getItemResource(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return getItemResource(item).orElse(null) == location;
    }

    public static List<Item> getItemsOfTag(TagKey<Item> tag) {
        return NeoMod.MC.getItemsOfTag(tag);
    }

    public static int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return NeoMod.MC.getEnchantmentLevel(stack, enchantment);
    }
    public static boolean isSilkTouched(ItemStack stack) {
        return NeoMod.MC.isSilkTouched(stack);
    }

    public static boolean canBreak(Item tool, BlockState block) {
        return NeoMod.MC.canBreak(tool, block);
    }

    public static Optional<Block> getBlock(String name) {
        return getBlock(getResourceLocation(name).orElse(null));
    }
    public static Optional<Block> getBlock(ResourceLocation location) {
        return NeoMod.MC.getBlock(location);
    }
    public static Optional<ResourceLocation> getBlockResource(Block block) {
        return NeoMod.MC.getBlockResource(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return getBlockResource(block).orElse(null) == location;
    }

    public static List<Block> getBlocksOfTag(TagKey<Block> tag) {
        return NeoMod.MC.getBlocksOfTag(tag);
    }

    public static Optional<BlockState> getBlockState(String name) {
        return getBlockState(getResourceLocation(name).orElse(null));
    }
    public static Optional<BlockState> getBlockState(ResourceLocation location) {
        return getBlock(location).map(Block::defaultBlockState);
    }
    public static Optional<ResourceLocation> getBlockStateResource(BlockState state) {
        return getBlockResource(state.getBlock());
    }
    public static boolean isValidBlockState(BlockState state, ResourceLocation location) {
        return getBlockResource(state.getBlock()).orElse(null) == location;
    }

    public static Optional<EntityType<?>> getEntityType(String name) {
        return getEntityType(getResourceLocation(name).orElse(null));
    }
    public static Optional<EntityType<?>> getEntityType(ResourceLocation location) {
        return NeoMod.MC.getEntityType(location);
    }
    public static Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        return NeoMod.MC.getEntityTypeResource(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return getEntityTypeResource(entityType).orElse(null) == location;
    }
    public static Iterable<ResourceLocation> getAllEntityTypes() {
        return NeoMod.MC.getAllEntityTypes();
    }

    public static <T extends Entity> T spawnEntity(ServerLevel level, EntityType<T> type, BlockPos pos) {
        return NeoMod.MC.spawnEntity(level, type, pos);
    }
    public static void teleportEntity(Entity entity, ServerLevel level, double ox, double oy, double oz, int ry, int rx) {
        NeoMod.MC.teleportEntity(entity, level, ox, oy, oz, ry, rx);
    }

    public static Optional<MobEffect> getMobEffect(String name) {
        return getMobEffect(getResourceLocation(name).orElse(null));
    }
    private static Optional<MobEffect> getMobEffect(ResourceLocation location) {
        return NeoMod.MC.getMobEffect(location);
    }
    private static Optional<ResourceLocation> getMobEffectResource(MobEffect effect) {
        return NeoMod.MC.getMobEffectResource(effect);
    }
    public static boolean isValidMobEffect(MobEffect effect, ResourceLocation location) {
        return getMobEffectResource(effect).orElse(null) == location;
    }
    public static MobEffectInstance getMobEffectInstance(MobEffect effect, int time) {
        return NeoMod.MC.effectFactory().apply(effect, time);
    }

    public static Iterable<Entity> allEntities(ServerLevel level) {
        return NeoMod.MC.iterateEntities(level);
    }

    public static void leash(Entity mob, Mob to) {
        NeoMod.MC.leash(mob, to);
    }

    public static Optional<MerchantOffer> getOfferOf(ItemStackProvider result, ItemStackProvider costA, ItemStackProvider costB, UniformInt uses) {
        return NeoMod.MC.getOfferOf(result, costA, costB, uses);
    }

    public enum ScoreboardSlots {
        LIST(0, "list"),
        SIDEBAR(1, "sidebar"),
        BELOW_NAME(2, "below_name"),
        TEAM_BLACK(3, "sidebar.team.black"),
        TEAM_DARK_BLUE(4, "sidebar.team.dark_blue"),
        TEAM_DARK_GREEN(5, "sidebar.team.dark_green"),
        TEAM_DARK_AQUA(6, "sidebar.team.dark_aqua"),
        TEAM_DARK_RED(7, "sidebar.team.dark_red"),
        TEAM_DARK_PURPLE(8, "sidebar.team.dark_purple"),
        TEAM_GOLD(9, "sidebar.team.gold"),
        TEAM_GRAY(10, "sidebar.team.gray"),
        TEAM_DARK_GRAY(11, "sidebar.team.dark_gray"),
        TEAM_BLUE(12, "sidebar.team.blue"),
        TEAM_GREEN(13, "sidebar.team.green"),
        TEAM_AQUA(14, "sidebar.team.aqua"),
        TEAM_RED(15, "sidebar.team.red"),
        TEAM_LIGHT_PURPLE(16, "sidebar.team.light_purple"),
        TEAM_YELLOW(17, "sidebar.team.yellow"),
        TEAM_WHITE(18, "sidebar.team.white");

        public final String value;
        public final int id;

        ScoreboardSlots(int id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    public static Objective createScoreboardObjective(Scoreboard scoreboard, String name, ObjectiveCriteria criteria, String title, ObjectiveCriteria.RenderType renderType) {
        return NeoMod.MC.createScoreboardObjective(scoreboard, name, criteria, title, renderType);
    }
    public static void setScoreboardDisplay(Scoreboard scoreboard, ScoreboardSlots slot, Objective objective) {
        NeoMod.MC.setScoreboardDisplay(scoreboard, slot, objective);
    }
    public static void setPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        NeoMod.MC.setPlayerScore(scoreboard, player, objective, amount);
    }
    public static void addPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        NeoMod.MC.addPlayerScore(scoreboard, player, objective, amount);
    }
    public static int getPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return NeoMod.MC.getPlayerScore(scoreboard, player, objective);
    }

    public static DustParticleOptions getDustParticle(Vector3f color, float value) {
        return NeoMod.MC.getDustParticle(color, value);
    }

    public static int getLevelMinY(ServerLevel level) {
        return NeoMod.MC.getLevelMinY(level);
    }
}