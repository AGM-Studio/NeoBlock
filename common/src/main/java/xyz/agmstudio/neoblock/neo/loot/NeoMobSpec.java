package xyz.agmstudio.neoblock.neo.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.platform.helpers.INBTHelper;
import xyz.agmstudio.neoblock.util.MessengerUtil;
import xyz.agmstudio.neoblock.util.MinecraftUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoMobSpec extends NeoItemSpec {
    private static final Pattern MOB_PATTERN = Pattern.compile("mob:(?<count>\\d+(-\\d+)?)?x?(?<id>[\\w:]+)(?:\\s+(?<chance>\\d+\\.?\\d*)%?)?");
    @NotNull private static final ResourceLocation DEFAULT = MinecraftUtil.parseResourceLocation("minecraft:pig");

    public static void load() {}

    private final EntityType<?> mob;

    public NeoMobSpec(EntityType<?> mob, UniformInt range, double chance, Item egg) {
        super(egg, range, chance);
        this.mob = mob;
    }

    public EntityType<?> getMob() {
        return mob;
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

        return Optional.of(new NeoMobSpec(entityType, range, chance, NeoBlock.REGISTRY.getMobTicket()));
    }

    public static Optional<EntityType<?>> getMobTradeEntity(ItemStack item) {
        if (item == null || !item.getItem().equals(NeoBlock.REGISTRY.getMobTicket())) return Optional.empty();

        CompoundTag tag = INBTHelper.Item.getItemTag(item);
        return MinecraftUtil.getEntityType(tag.getString("neoMobType"));
    }

    public static boolean handlePossibleMobTrade(ItemStack item, ServerLevel level) {
        Optional<EntityType<?>> mob = getMobTradeEntity(item);
        if (mob.isEmpty()) return false;

        MessengerUtil.sendInstantMessage("message.neoblock.trades.mob", level, true, item.getCount(), mob.get().getDescription());
        WorldData.getWorldStatus().addTradedMob(mob.get(), item.getCount());
        item.setCount(0);

        return true;
    }

    public static class TradeTicket extends Item {

        public TradeTicket(Properties properties) {
            super(properties);
        }

        @Override public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server)
                handlePossibleMobTrade(stack, server);

            return InteractionResultHolder.success(stack);
        }

        @Override public @NotNull Component getName(@NotNull ItemStack stack) {
            Optional<EntityType<?>> mob = getMobTradeEntity(stack);
            return mob.map(
                    t -> Component.translatable("item.neoblock.mob_ticket.of", t.getDescription())).orElseGet(
                            () -> Component.translatable("item.neoblock.mob_ticket")
            );
        }

        @Override public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
            Optional<EntityType<?>> mob = getMobTradeEntity(stack);
            if (mob.isEmpty()) return;
            components.add(
                    Component.translatable("tooltip.neoblock.spawn_lore", mob.get().getDescription())
                            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)
            );
        }
    }
}
