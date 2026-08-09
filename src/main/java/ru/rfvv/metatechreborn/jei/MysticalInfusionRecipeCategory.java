package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.integration.mystical.MysticalInfusionSupport;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

public final class MysticalInfusionRecipeCategory implements IRecipeCategory<MysticalInfusionSupport.View> {
    public static final RecipeType<MysticalInfusionSupport.View> TYPE = RecipeType.create(
            MetaTechReborn.MOD_ID, "mystical_infusion", MysticalInfusionSupport.View.class);
    private final IDrawable background;
    private final IDrawable icon;

    public MysticalInfusionRecipeCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(132, 92);
        icon = gui.createDrawableItemLike(ModMysticalInfusion.ASSEMBLER_ITEM.get());
    }
    @Override public @NotNull RecipeType<MysticalInfusionSupport.View> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.literal("Mystical Agriculture — Infusion Altar"); }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull MysticalInfusionSupport.View recipe,
                          @NotNull IFocusGroup focuses) {
        int[] ring = {0,1,2,3,5,6,7,8};
        builder.addInputSlot(38, 38).setStandardSlotBackground().addIngredients(recipe.ingredients().get(0));
        for (int i = 0; i < 8; i++) {
            int grid = ring[i];
            int ingredient = i + 1;
            if (ingredient >= recipe.ingredients().size() || recipe.ingredients().get(ingredient).isEmpty()) continue;
            builder.addInputSlot(2 + (grid % 3) * 36, 2 + (grid / 3) * 36)
                    .setStandardSlotBackground().addIngredients(recipe.ingredients().get(ingredient));
        }
        builder.addOutputSlot(108, 38).setOutputSlotBackground().addItemStack(recipe.output());
    }
}
