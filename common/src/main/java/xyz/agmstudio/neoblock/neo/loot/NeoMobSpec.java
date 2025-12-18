package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.INBTHelper;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.PatternUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoMobSpec extends NeoItemSpec {
    private static final Pattern MOB_PATTERN =
            PatternUtil.literal("mob:").then(PatternUtil.RANGE.optional()).then(PatternUtil.NAMESPACE).then(PatternUtil.CHANCE.optional()).build(false);
    @NotNull private static final ResourceLocation DEFAULT = MinecraftUtil.parseResourceLocation("minecraft:pig");

    public static void load() {}

    private final EntityType<?> mob;

    public NeoMobSpec(EntityType<?> mob, UniformInt range, double chance) {
        super(null, range, chance);
        this.mob = mob;
    }

    public EntityType<?> getMob() {
        return mob;
    }

    @Override public Item getItem() {
        return NeoBlock.REGISTRY.getMobTicket();
    }

    @Override public ItemStack modify(ItemStack item) {
        CompoundTag tag = INBTHelper.Item.getItemTag(item);

        @Nullable ResourceLocation location = MinecraftUtil.getEntityTypeResource(mob).orElse(null);
        tag.putString("neoMobType", location != null ? location.toString() : DEFAULT.toString());

        INBTHelper.Item.setItemTag(item, tag);
        return item;
    }

    @Override public ResourceLocation getResource() {
        return MinecraftUtil.getEntityTypeResource(mob).orElse(DEFAULT);
    }
    @Override
    public String getId() {
        return "mob:" + getResource();
    }

    public static Optional<NeoMobSpec> parseMob(String input) {
        if (input == null) return Optional.empty();

        Matcher matcher = MOB_PATTERN.matcher(input.trim().toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        String id = matcher.group("id");
        EntityType<?> entityType = EntityType.byString(id).orElse(null);
        if (entityType == null) return Optional.empty();

        UniformInt range = StringUtil.parseRange(matcher.group("count"));
        double chance = StringUtil.parseChance(matcher.group("chance"));

        return Optional.of(new NeoMobSpec(entityType, range, chance));
    }

    public static Optional<EntityType<?>> getMobTradeEntity(ItemStack item) {
        if (item == null || !item.getItem().equals(NeoBlock.REGISTRY.getMobTicket())) return Optional.empty();

        CompoundTag tag = INBTHelper.Item.getItemTag(item);
        return MinecraftUtil.getEntityType(tag.getString("neoMobType"));
    }

    public static boolean handlePossibleMobTrade(ItemStack item, ServerLevel level) {
        Optional<EntityType<?>> mob = getMobTradeEntity(item);
        if (mob.isEmpty()) return false;

        NeoBlock.sendInstantMessage("message.neoblock.trades.mob", level, true, item.getCount(), mob.get().getDescription());
        WorldData.getWorldStatus().addTradedMob(mob.get(), item.getCount());
        item.setCount(0);

        return true;
    }

    public static ItemStack of(EntityType<?> mob, int count) {
        ItemStack item = new ItemStack(NeoBlock.REGISTRY.getMobTicket(), count);
        CompoundTag tag = INBTHelper.Item.getItemTag(item);

        @Nullable ResourceLocation location = MinecraftUtil.getEntityTypeResource(mob).orElse(null);
        tag.putString("neoMobType", location != null ? location.toString() : DEFAULT.toString());

        INBTHelper.Item.setItemTag(item, tag);
        return item;
    }

    public static abstract class TradeTicket extends Item {
        public TradeTicket(Properties properties) {
            super(properties);
        }

        @Override public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
            ItemStack stack = context.getItemInHand();
            if (context.getLevel() instanceof ServerLevel server)
                handlePossibleMobTrade(stack, server);

            return InteractionResult.SUCCESS;
        }

        @Override public @NotNull Component getName(@NotNull ItemStack stack) {
            Optional<EntityType<?>> mob = getMobTradeEntity(stack);
            return mob.map(
                    t -> Component.translatable("item.neoblock.mob_ticket.of", t.getDescription())).orElseGet(
                            () -> Component.translatable("item.neoblock.mob_ticket")
            );
        }

        public @NotNull List<Component> getLore(@NotNull ItemStack stack) {
            Optional<EntityType<?>> mob = getMobTradeEntity(stack);
            return mob.<List<Component>>map(entityType -> List.of(
                    Component.translatable("tooltip.neoblock.spawn_lore", entityType.getDescription())
                            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
            )).orElseGet(List::of);
        }
    }
}
