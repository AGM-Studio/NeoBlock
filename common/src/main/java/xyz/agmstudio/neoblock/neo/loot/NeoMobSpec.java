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
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.NeoMC;
import xyz.agmstudio.neocore.NeoNBT;
import xyz.agmstudio.neocore.util.StringUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoMobSpec extends NeoItemSpec {
    private static final Pattern MOB_PATTERN =
            StringUtil.literal("mob:").then(StringUtil.RANGE.optional()).then(StringUtil.NAMESPACE).then(StringUtil.CHANCE.optional()).build(false);
    @NotNull private static final ResourceLocation DEFAULT = NeoMC.parseResourceLocation("minecraft:pig");

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
        return NeoBlockMod.getRegistry().getMobTicket();
    }

    @Override public ItemStack modify(ItemStack item) {
        CompoundTag tag = NeoNBT.Item.getItemTag(item);

        @Nullable ResourceLocation location = NeoMC.getEntityTypeResource(mob).orElse(null);
        tag.putString("neoMobType", location != null ? location.toString() : DEFAULT.toString());

        NeoNBT.Item.setItemTag(item, tag);
        return item;
    }
    public ItemStack modifyForTrader(ItemStack item, NeoBlock block) {
        CompoundTag tag = NeoNBT.Item.getItemTag(item);

        tag.putString("neoBlockId", block.getID());

        NeoNBT.Item.setItemTag(item, tag);
        return item;
    }


    @Override public ResourceLocation getResource() {
        return NeoMC.getEntityTypeResource(mob).orElse(DEFAULT);
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
        if (item == null || !item.getItem().equals(NeoBlockMod.getRegistry().getMobTicket())) return Optional.empty();

        CompoundTag tag = NeoNBT.Item.getItemTag(item);
        return NeoMC.getEntityType(tag.getString("neoMobType"));
    }
    public static Optional<NeoBlock> getMobTradeBlock(ItemStack item) {
        if (item == null || !item.getItem().equals(NeoBlockMod.getRegistry().getMobTicket())) return Optional.empty();

        CompoundTag tag = NeoNBT.Item.getItemTag(item);
        String blockId = tag.getString("neoBlockId");
        return WorldManager.getBlocks().stream().filter(b -> Objects.equals(b.getID(), blockId)).findFirst();
    }

    public static boolean handlePossibleMobTrade(ItemStack item, ServerLevel level) {
        Optional<EntityType<?>> mob = getMobTradeEntity(item);
        Optional<NeoBlock> block = getMobTradeBlock(item);
        if (mob.isEmpty() || block.isEmpty()) return false;

        NeoBlockMod.sendInstantMessage("message.neoblock.trades.mob", level, true, item.getCount(), mob.get().getDescription());
        block.get().addTradedMob(mob.get(), item.getCount());
        item.setCount(0);

        return true;
    }

    public static ItemStack of(EntityType<?> mob, NeoBlock block, int count) {
        ItemStack item = new ItemStack(NeoBlockMod.getRegistry().getMobTicket(), count);
        CompoundTag tag = NeoNBT.Item.getItemTag(item);

        @Nullable ResourceLocation location = NeoMC.getEntityTypeResource(mob).orElse(null);
        tag.putString("neoMobType", location != null ? location.toString() : DEFAULT.toString());
        tag.putString("neoBlockId", block.getID());

        NeoNBT.Item.setItemTag(item, tag);
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
                            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                    Component.literal("Block: " + getMobTradeBlock(stack).map(NeoBlock::getID).orElse(null))
            )).orElseGet(List::of);
        }
    }
}