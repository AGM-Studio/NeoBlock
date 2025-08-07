package xyz.agmstudio.neoblock.compatibility.minecraft;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.loot.NeoItemSpec;
import xyz.agmstudio.neoblock.neo.world.WorldData;

import java.util.Optional;

public final class MerchantAPI {
    private static ItemStack toItemStack(NeoItemSpec item, RandomSource random) {
        ItemStack stack = new ItemStack(item.getItem(), item.getRange().sample(random));
        return item.modify(stack);
    }

    private static ItemCost toItemCost(NeoItemSpec item, RandomSource random) {
        return new ItemCost(item.getItem(), item.getRange().sample(random));
    }

    public static Optional<MerchantOffer> getOfferOf(NeoItemSpec result, NeoItemSpec costA, NeoItemSpec costB, UniformInt uses) {
        @NotNull final RandomSource RNG = WorldData.getRandom();
        @NotNull final Item AIR = net.minecraft.world.item.Items.AIR;

        ItemStack r = toItemStack(result, RNG);
        ItemCost a = toItemCost(costA, RNG);
        Optional<ItemCost> b = costB != null ? Optional.of(toItemCost(costB, RNG)) : Optional.empty();

        if (r.getItem() == AIR || a.itemStack().getItem() == AIR) return Optional.empty();
        return Optional.of(new MerchantOffer(a, b, r, uses.sample(RNG), 0, 0));
    }
}
