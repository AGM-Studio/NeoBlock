package xyz.agmstudio.neoblock.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.platform.Services;
import xyz.agmstudio.neoblock.platform.helpers.IMinecraftHelper;

import java.util.Optional;

/**
 * This class a utility class and based on the version of minecraft build should help to keep all code similar
 */
public final class MinecraftUtil {
    private static final IMinecraftHelper HELPER = Services.load(IMinecraftHelper.class);

    public static @NotNull ResourceLocation parseResourceLocation(String name) {
        return HELPER.parseResourceLocation(name);
    }
    public static Optional<ResourceLocation> getResourceLocation(String name) {
        return HELPER.getResourceLocation(name);
    }
    public static ResourceLocation createResourceLocation(String namespace, String path) {
        return HELPER.createResourceLocation(namespace, path);
    }

    public static Optional<Item> getItem(String name) {
        return getItem(getResourceLocation(name).orElse(null));
    }
    public static Optional<Item> getItem(ResourceLocation location) {
        return HELPER.getItem(location);
    }
    public static Optional<ResourceLocation> getItemResource(Item item) {
        return HELPER.getItemResource(item);
    }
    public static boolean isValidItem(Item item, ResourceLocation location) {
        return getItemResource(item).orElse(null) == location;
    }

    public static int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return HELPER.getEnchantmentLevel(stack, enchantment);
    }
    public static boolean isSilkTouched(ItemStack stack) {
        return HELPER.isSilkTouched(stack);
    }

    public static boolean canBreak(TieredItem tool, Block block) {
        return HELPER.canBreak(tool, block);
    }

    public static Optional<Block> getBlock(String name) {
        return getBlock(getResourceLocation(name).orElse(null));
    }
    public static Optional<Block> getBlock(ResourceLocation location) {
        return HELPER.getBlock(location);
    }
    public static Optional<ResourceLocation> getBlockResource(Block block) {
        return HELPER.getBlockResource(block);
    }
    public static boolean isValidBlock(Block block, ResourceLocation location) {
        return getBlockResource(block).orElse(null) == location;
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
        return HELPER.getEntityType(location);
    }
    public static Optional<ResourceLocation> getEntityTypeResource(EntityType<?> type) {
        return HELPER.getEntityTypeResource(type);
    }
    public static boolean isValidEntityType(EntityType<?> entityType, ResourceLocation location) {
        return getEntityTypeResource(entityType).orElse(null) == location;
    }

    public static Optional<MobEffect> getMobEffect(String name) {
        return getMobEffect(getResourceLocation(name).orElse(null));
    }
    private static Optional<MobEffect> getMobEffect(ResourceLocation location) {
        return HELPER.getMobEffect(location);
    }
    private static Optional<ResourceLocation> getMobEffectResource(MobEffect effect) {
        return HELPER.getMobEffectResource(effect);
    }
    public static boolean isValidMobEffect(MobEffect effect, ResourceLocation location) {
        return getMobEffectResource(effect).orElse(null) == location;
    }

    public static MobEffectInstance getMobEffectInstance(MobEffect effect, int time) {
        return HELPER.effectFactory().apply(effect, time);
    }

    public static Iterable<Entity> allEntities(ServerLevel level) {
        return HELPER.iterateEntities(level);
    }

    public static void leash(Entity mob, Mob to) {
        HELPER.leash(mob, to);
    }

    public static Optional<MerchantOffer> getOfferOf(NeoItemSpec result, NeoItemSpec costA, NeoItemSpec costB, UniformInt uses) {
        return HELPER.getOfferOf(result, costA, costB, uses);
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
        return HELPER.createScoreboardObjective(scoreboard, name, criteria, title, renderType);
    }
    public static void setScoreboardDisplay(Scoreboard scoreboard, ScoreboardSlots slot, Objective objective) {
        HELPER.setScoreboardDisplay(scoreboard, slot, objective);
    }
    public static void setPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        HELPER.setPlayerScore(scoreboard, player, objective, amount);
    }
    public static void addPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective, int amount) {
        HELPER.addPlayerScore(scoreboard, player, objective, amount);
    }
    public static int getPlayerScore(Scoreboard scoreboard, ServerPlayer player, Objective objective) {
        return HELPER.getPlayerScore(scoreboard, player, objective);
    }
}
