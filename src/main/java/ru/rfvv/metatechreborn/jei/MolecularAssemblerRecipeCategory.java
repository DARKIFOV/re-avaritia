package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.registry.ModItems;

public final class MolecularAssemblerRecipeCategory implements IRecipeCategory<MolecularAssemblerRecipe> {
    public static final RecipeType<MolecularAssemblerRecipe> TYPE = RecipeType.create(
            MetaTechReborn.MOD_ID, "molecular_assembling", MolecularAssemblerRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;
    public MolecularAssemblerRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(202, 166);
        icon = helper.createDrawableItemLike(ModItems.MOLECULAR_ASSEMBLER_9X9.get());
    }
    @Override public @NotNull RecipeType<MolecularAssemblerRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.metatech_reborn.molecular_assembling"); }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }
    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull MolecularAssemblerRecipe recipe,
                                    @NotNull IFocusGroup focuses) {
        var grid = recipe.getGridIngredients();
        for (int row = 0; row < 9; row++) for (int column = 0; column < 9; column++) {
            Ingredient ingredient = grid.get(column + row * 9);
            if (!ingredient.isEmpty()) builder.addInputSlot(1 + column * 18, 1 + row * 18)
                    .setStandardSlotBackground().addIngredients(ingredient);
        }
        builder.addOutputSlot(181, 73).setOutputSlotBackground().addItemStack(recipe.result());
    }
}
