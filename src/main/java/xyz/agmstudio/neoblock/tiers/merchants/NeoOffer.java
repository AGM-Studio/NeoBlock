package xyz.agmstudio.neoblock.tiers.merchants;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.MerchantOffer;
import xyz.agmstudio.neoblock.data.Range;
import xyz.agmstudio.neoblock.tiers.WorldData;
import xyz.agmstudio.neoblock.util.MessagingUtil;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.Optional;

public final class NeoOffer {
    private final NeoItem result;
    private final NeoItem costA;
    private final NeoItem costB;
    private final Range uses;

    public NeoOffer(NeoItem result, NeoItem costA, NeoItem costB, Range uses) {
        this.result = result;
        this.costA = costA;
        this.costB = costB;
        this.uses = uses;
    }

    public static NeoOffer parse(String trade) {
        String[] parts = trade.split("\\s*;\\s*");
        if (parts.length < 2) return null;

        NeoItem result = NeoItem.parse(parts[0]);
        NeoItem costA = NeoItem.parse(parts[1]);
        NeoItem costB = null;
        Range uses = null;

        if (parts.length == 3) {
            uses = StringUtil.parseRange(parts[2]);
            if (uses == null) costB = NeoItem.parse(parts[2]);
        } else if (parts.length > 3) {
            costB = NeoItem.parse(parts[2]);
            uses = StringUtil.parseRange(parts[3]);
        }

        return new NeoOffer(result, costA, costB, uses);
    }

    public String toString() {
        String codec = result + "; " + costA + ";";
        if (costB != null) codec += " " + costB + ";";
        return codec + uses.toString();
    }

    public MerchantOffer getOffer() {
        return new MerchantOffer(costA.getCost(), costB == null ? Optional.empty() : Optional.of(costB.getCost()), result.getStack(), uses.get(), 0, 0);
    }

    public static EntityType<?> getMobTradeEntity(ItemStack item) {
        if (item == null) return null;

        CustomData data = item.getComponents().get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;

        CompoundTag tag = data.copyTag();
        if (!tag.getBoolean("isNeoMob")) return null;

        String type = tag.getString("neoMobType");
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type));
    }

    public static boolean handlePossibleMobTrade(ItemStack item, ServerLevel level) {
        EntityType<?> mob = getMobTradeEntity(item);
        if (mob == null) return false;

        MessagingUtil.sendInstantMessage("message.neoblock.trades.mob", level, true, item.getCount(), mob.getDescription());
        WorldData.addTradedMob(mob, item.getCount());
        item.setCount(0);

        return true;
    }
}
