package ru.rfvv.metatechreborn.jei;

import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;

public final class ExtremePatternEncoderRecipeGrids {
    private ExtremePatternEncoderRecipeGrids() {}

    public static NonNullList<ItemStack> fromMolecular(MolecularAssemblerRecipe recipe) {
        return chooseItems(recipe.getGridIngredients());
    }

    public static NonNullList<ItemStack> fromAvaritia(ITierCraftingRecipe recipe) {
        NonNullList<ItemStack> grid = NonNullList.withSize(
                ExtremePatternEncoderBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedTableCraftingRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            int offsetX = Math.floorDiv(9 - width, 2);
            int offsetY = Math.floorDiv(9 - height, 2);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int ingredientIndex = x + y * width;
                    if (ingredientIndex >= ingredients.size()) continue;
                    grid.set(offsetX + x + (offsetY + y) * 9,
                            chooseItem(ingredients.get(ingredientIndex)));
                }
            }
            return grid;
        }

        int limit = Math.min(ingredients.size(), grid.size());
        for (int slot = 0; slot < limit; slot++) {
            grid.set(slot, chooseItem(ingredients.get(slot)));
        }
        return grid;
    }

    private static NonNullList<ItemStack> chooseItems(NonNullList<Ingredient> ingredients) {
        NonNullList<ItemStack> grid = NonNullList.withSize(
                ExtremePatternEncoderBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        int limit = Math.min(ingredients.size(), grid.size());
        for (int slot = 0; slot < limit; slot++) {
            grid.set(slot, chooseItem(ingredients.get(slot)));
        }
        return grid;
    }

    private static ItemStack chooseItem(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] options = ingredient.getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        ItemStack selected = options[0].copy();
        selected.setCount(1);
        return selected;
    }
}
