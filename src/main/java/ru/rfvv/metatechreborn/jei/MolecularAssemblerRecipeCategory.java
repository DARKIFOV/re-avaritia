package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.registry.ModItems;

public final class MolecularAssemblerRecipeCategory implements IRecipeCategory<MolecularAssemblerRecipe> {
    public static final RecipeType<MolecularAssemblerRecipe> TYPE =
            RecipeType.create(MetaTechReborn.MOD_ID, "molecular_assembling", MolecularAssemblerRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MolecularAssemblerRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(202, 166);
        this.icon = guiHelper.createDrawableItemLike(ModItems.MOLECULAR_ASSEMBLER_9X9.get());
    }

    @Override
    public @NotNull RecipeType<MolecularAssemblerRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.metatech_reborn.molecular_assembling");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull MolecularAssemblerRecipe recipe,
                          @NotNull IFocusGroup focuses) {
        var grid = recipe.getGridIngredients();
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                Ingredient ingredient = grid.get(column + row * 9);
                if (!ingredient.isEmpty()) {
                    builder.addInputSlot(1 + column * 18, 1 + row * 18)
                            .setStandardSlotBackground()
                            .addIngredients(ingredient);
                }
            }
        }
        builder.addOutputSlot(181, 73)
                .setOutputSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(@NotNull MolecularAssemblerRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.literal(recipe.time() + " t"),
                166, 105, 0xFF404040, false);
        graphics.drawString(font,
                Component.literal(recipe.energyPerTick() + " FE/t"),
                166, 118, 0xFF404040, false);
    }
}
