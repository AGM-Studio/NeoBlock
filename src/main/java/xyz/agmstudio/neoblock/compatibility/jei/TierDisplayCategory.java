package xyz.agmstudio.neoblock.compatibility.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.neo.world.WorldData;
import xyz.agmstudio.neoblock.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class TierDisplayCategory implements IRecipeCategory<TierDisplay> {
    public static final RecipeType<TierDisplay> TYPE = RecipeType.create("neoblock", "tier_display", TierDisplay.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;

    private final int rows;
    private final int height;

    @Override
    public @NotNull RecipeType<TierDisplay> getRecipeType() {
        return TYPE;
    }

    public TierDisplayCategory(IGuiHelper helper) {
        int max = WorldData.getWorldTiers().stream().mapToInt(tier -> tier.getBlocks().size()).max().orElse(0);
        rows = max % 9 == 0 ? max / 9 : max / 9 + 1;
        height = 78 + 18 * rows;
        background = helper.createBlankDrawable(166, height);
        slot = helper.getSlotDrawable();
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, NeoJEIPlugin.CATALYST.toStack());
    }

    @Override public @NotNull Component getTitle() {
        return Component.translatable("jei.neoblock.title");
    }

    @Override public void draw(@NotNull TierDisplay display, @NotNull IRecipeSlotsView view, @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        for (NeoJEIPlugin.TextBox box: display.getTextBoxes()) box.draw(graphics);
    }

    @Override public IDrawable getIcon() { return icon; }
    @SuppressWarnings("removal")
    @Override public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull TierDisplay display, @NotNull IFocusGroup focuses) {
        NeoJEIPlugin.recalculate();
        Item focus = focuses.getFocuses(RecipeIngredientRole.OUTPUT)
                .filter(f -> f.getRole() == RecipeIngredientRole.OUTPUT)
                .map(f -> f.getTypedValue().getItemStack().orElse(ItemStack.EMPTY))
                .findFirst().orElse(ItemStack.EMPTY).getItem();

        if (focus != Items.AIR && display.getBlocks().containsKey(focus))
            addBlockGeneration(builder, display, focus, 146, 2, 1);

        List<Map.Entry<Item, Integer>> blocks = new ArrayList<>(display.getBlocks().entrySet());
        for (int i = 0; i < rows * 9; i++) addBlockGeneration(builder, display,
                i < blocks.size() ? blocks.get(i).getKey() : null,
                (i % 9) * 18 + 3,
                78 + (i / 9) * 18,
                i < blocks.size() ? blocks.get(i).getValue() : 1
        );
    }

    private void addBlockGeneration(@NotNull IRecipeLayoutBuilder builder, @NotNull TierDisplay display, Item item, int x, int y, int count) {
        IRecipeSlotBuilder build = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setBackground(slot, -1, -1);
        if (item == null || item == Items.AIR) return;
        build.addItemStack(new ItemStack(item, count)).addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("tooltip.neoblock.tier_chance", StringUtil.percentage(display.getChance(item), 2)));
            tooltip.add(Component.translatable("tooltip.neoblock.total_chance", StringUtil.percentage(NeoJEIPlugin.getTotalChance(item), 2)));
        });
    }
}
