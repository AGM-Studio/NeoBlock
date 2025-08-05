package xyz.agmstudio.neoblock.compatibility.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.data.TierData;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.neo.world.WorldTier;
import xyz.agmstudio.neoblock.minecraft.MinecraftAPI;

import java.util.*;

@JeiPlugin
public class NeoJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = MinecraftAPI.parseResourceLocation("neoblock:jei_plugin");

    public static final int WHITE_COLOR = 0xFFFFFF;
    public static final int RED_COLOR = 0xFF5555;
    public static final int GREEN_COLOR = 0x33BB33;
    public static final int BLUE_COLOR = 0x5555FF;
    public static final int GRAY_COLOR = 0x888888;
    public static final int YELLOW_COLOR = 0xFFFF55;
    public static final int ORANGE_COLOR = 0xFFAA00;

    private static IJeiRuntime runtime = null;
    protected static final DeferredBlock<Block> CATALYST_BLOCK = NeoBlockMod.BLOCKS.registerSimpleBlock("neoblock");
    protected static final DeferredItem<BlockItem> CATALYST = NeoBlockMod.ITEMS.register(
            "neoblock", () -> new BlockItem(CATALYST_BLOCK.get(), new Item.Properties())
    );

    public static IJeiRuntime getRuntime() {
        return runtime;
    }
    public static IFocusFactory getFocusFactory() {
        return runtime.getJeiHelpers().getFocusFactory();
    }

    private static final List<TierDisplay> displays = new ArrayList<>(TierData.stream().map(TierDisplay::new).toList());
    private static final LinkedHashMap<Item, Double> chances = new LinkedHashMap<>();
    public static void recalculate() {
        chances.clear();
        int weights = WorldData.totalWeight();
        for (TierDisplay display: displays) {
            WorldTier tier = display.getData().getWorldTier();
            if (tier != null && tier.isEnabled()) for (Map.Entry<Item, Double> entry : display.getChances())
                chances.merge(entry.getKey(), entry.getValue() * tier.getWeight() / weights, Double::sum);
        }
    }

    public static double getTotalChance(Item item) {
        return chances.getOrDefault(item, 0.0);
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() { return Objects.requireNonNull(ID); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new TierDisplayCategory(helper));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(CATALYST.toStack(), TierDisplayCategory.TYPE);
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

    public static TextBox box(String key, int x, int y, boolean color, Object... args) {
        return new TextBox(key, x, y, color ? GREEN_COLOR : RED_COLOR, args);
    }
    public static TextBox box(String key, int x, int y, int color, Object... args) {
        return new TextBox(key, x, y, color, args);
    }
    public static void addBox(Collection<TextBox> boxes, String key, int x, int y, boolean color, Object... args) {
        boxes.add(new TextBox(key, x, y, color ? GREEN_COLOR : RED_COLOR, args));
    }
    public static void addBox(Collection<TextBox> boxes, String key, int x, int y, int color, Object... args) {
        boxes.add(new TextBox(key, x, y, color, args));
    }
    public record TextBox(String key, int x, int y, int color, Object... args) {
        public int draw(GuiGraphics graphics) {
            return graphics.drawString(Minecraft.getInstance().font, Component.translatable(key, args), x, y, color, false);
        }
    }
}
