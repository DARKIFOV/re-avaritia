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
import ru.rfvv.metatechreborn.recipe.NeutroniumCombinerRecipe;
import ru.rfvv.metatechreborn.registry.ModItems;

public final class NeutroniumCombinerRecipeCategory implements IRecipeCategory<NeutroniumCombinerRecipe> {
    public static final RecipeType<NeutroniumCombinerRecipe> TYPE = RecipeType.create(
            MetaTechReborn.MOD_ID, "neutronium_combining", NeutroniumCombinerRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;
    public NeutroniumCombinerRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(126, 46);
        icon = helper.createDrawableItemLike(ModItems.NEUTRONIUM_COMBINER.get());
    }
    @Override public @NotNull RecipeType<NeutroniumCombinerRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() { return Component.translatable("jei.metatech_reborn.neutronium_combining"); }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }
    @Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull NeutroniumCombinerRecipe recipe,
                                    @NotNull IFocusGroup focuses) {
        builder.addInputSlot(5, 14).setStandardSlotBackground().addIngredients(recipe.collector());
        builder.addOutputSlot(103, 14).setOutputSlotBackground().addItemStack(recipe.result());
    }
}
