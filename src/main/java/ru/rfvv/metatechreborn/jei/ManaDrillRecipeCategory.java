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
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.recipe.ManaDrillRecipe;
import ru.rfvv.metatechreborn.registry.ModItems;

public final class ManaDrillRecipeCategory implements IRecipeCategory<ManaDrillRecipe> {
    public static final RecipeType<ManaDrillRecipe> TYPE = RecipeType.create(
            MetaTechReborn.MOD_ID, "mana_drill_generating", ManaDrillRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ManaDrillRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(168, 76);
        icon = guiHelper.createDrawableItemLike(ModItems.MANA_DRILL.get());
    }

    @Override public @NotNull RecipeType<ManaDrillRecipe> getRecipeType() { return TYPE; }
    @Override public @NotNull Component getTitle() {
        return Component.translatable("jei.metatech_reborn.mana_drill_generating");
    }
    @Override public @NotNull IDrawable getBackground() { return background; }
    @Override public @NotNull IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ManaDrillRecipe recipe,
                          @NotNull IFocusGroup focuses) {
        builder.addInputSlot(4, 28).setStandardSlotBackground()
                .addIngredients(recipe.getIngredients().get(0));
        int shown = Math.min(7, recipe.drops().size());
        for (int index = 0; index < shown; index++) {
            int column = index % 4;
            int row = index / 4;
            builder.addOutputSlot(74 + column * 22, 17 + row * 22)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.drops().get(index).preview());
        }
    }

    @Override
    public void draw(@NotNull ManaDrillRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal(recipe.time() + " t"),
                28, 22, 0xFF404040, false);
        graphics.drawString(font, Component.literal(recipe.manaCost() + " Mana"),
                28, 38, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("jei.metatech_reborn.mana_drill.upgrades"),
                4, 61, 0xFF404040, false);
    }
}
