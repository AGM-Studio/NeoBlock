package xyz.agmstudio.neoblock.compatibility.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;

public class TierDisplayCategory implements IRecipeCategory<TierDisplay> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    public static final RecipeType<TierDisplay> TYPE =
            RecipeType.create("neoblock", "tier_display", TierDisplay.class);

    @Override
    public @NotNull RecipeType<TierDisplay> getRecipeType() {
        return TYPE;
    }

    public TierDisplayCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(150, 60); // size can be adjusted
        slot = guiHelper.getSlotDrawable();
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.NETHER_STAR));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.literal("Tiered Blocks");
    }

    @Override public void draw(@NotNull TierDisplay display, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);

        guiGraphics.drawString(Minecraft.getInstance().font, display.getTierName(), 10, 10, 2);
    }

    @Override public IDrawable getIcon() { return icon; }
    @SuppressWarnings("removal")
    @Override public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, TierDisplay display, @NotNull IFocusGroup focuses) {
        int x = 50;
        int y = 10;

        NeoBlockJEIPlugin.recalculate();
        NeoBlockMod.LOGGER.debug("Recalculating the chances!");
        int sum = display.getBlocks().values().stream().mapToInt(weight -> weight).sum();
        for (ItemStack block : display.getBlocks().sequencedKeySet()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(block)
                    .setBackground(slot, -1, -1)
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("tooltip.neoblock.tier_chance", display.getChance(block)));
                        tooltip.add(Component.translatable("tooltip.neoblock.total_chance", NeoBlockJEIPlugin.getTotalChance(block)));
                    });

            x += 20;
            if (x >= 130) {
                x = 50;
                y += 20;
            }
        }
    }
}
