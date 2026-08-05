package ru.rfvv.metatechreborn.jei;

import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;

public final class ExtremePatternEncoderRecipeGrids {
    private static final int FULL_GRID_SIZE = 9;

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
            int offsetX = Math.floorDiv(FULL_GRID_SIZE - width, 2);
            int offsetY = Math.floorDiv(FULL_GRID_SIZE - height, 2);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int ingredientIndex = x + y * width;
                    if (ingredientIndex >= ingredients.size()) continue;
                    grid.set(offsetX + x + (offsetY + y) * FULL_GRID_SIZE,
                            chooseItem(ingredients.get(ingredientIndex)));
                }
            }
            return grid;
        }

        int tierSize = tierGridSize(recipe.getTier());
        int offset = (FULL_GRID_SIZE - tierSize) / 2;
        int limit = Math.min(ingredients.size(), tierSize * tierSize);
        for (int slot = 0; slot < limit; slot++) {
            int x = slot % tierSize;
            int y = slot / tierSize;
            grid.set(offset + x + (offset + y) * FULL_GRID_SIZE,
                    chooseItem(ingredients.get(slot)));
        }
        return grid;
    }

    private static int tierGridSize(int tier) {
        if (tier < 1 || tier > 4) return FULL_GRID_SIZE;
        return tier * 2 + 1;
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
