package xyz.agmstudio.neoblock.compatibility.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.*;

@JeiPlugin
public class NeoBlockJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = MinecraftUtil.getResourceLocation("neoblock:jei_plugin");
    private static IJeiRuntime runtime = null;

    public static IJeiRuntime getRuntime() {
        return runtime;
    }
    public static IFocusFactory getFocusFactory() {
        return runtime.getJeiHelpers().getFocusFactory();
    }

    private static final List<TierDisplay> displays = new ArrayList<>(TierData.stream().map(TierDisplay::new).toList());
    private static final LinkedHashMap<ItemStack, Double> chances = new LinkedHashMap<>();
    public static void recalculate() {
        chances.clear();
        int sum = WorldData.getTiers().stream().filter(WorldTier::isEnabled).mapToInt(WorldTier::getWeight).sum();
        for (TierDisplay display: displays) {
            display.recalculate();
            WorldTier tier = display.getData().getWorldTier();
            if (tier != null && tier.isEnabled()) {
                double scale = 100.0 * tier.getWeight() / sum;
                for (Map.Entry<ItemStack, Double> entry : display.getChances())
                    chances.merge(entry.getKey(), Math.round(entry.getValue() * scale) / 100.0, Double::sum);
            }
        }
    }

    public static double getTotalChance(ItemStack item) {
        return chances.getOrDefault(item, 0.0);
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() { return Objects.requireNonNull(ID); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new TierDisplayCategory(guiHelper));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.BEDROCK), TierDisplayCategory.TYPE);
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        displays.clear();
        displays.addAll(TierData.stream().map(TierDisplay::new).toList());

        registration.addRecipes(TierDisplayCategory.TYPE, displays);
    }

    @Override public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }
    @Override public void onRuntimeUnavailable() {
        runtime = null;
    }
}
