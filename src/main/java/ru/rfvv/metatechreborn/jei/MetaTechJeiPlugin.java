package ru.rfvv.metatechreborn.jei;

import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.init.compat.jei.category.tables.EndCraftingTableCategory;
import committee.nova.mods.avaritia.init.compat.jei.category.tables.ExtremeCraftingTableCategory;
import committee.nova.mods.avaritia.init.compat.jei.category.tables.NetherCraftingTableCategory;
import committee.nova.mods.avaritia.init.compat.jei.category.tables.SculkCraftingTableCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;
import ru.rfvv.metatechreborn.registry.ModRecipes;

@JeiPlugin
public final class MetaTechJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            new ResourceLocation(MetaTechReborn.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MolecularAssemblerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ManaDrillRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        registration.addRecipes(
                MolecularAssemblerRecipeCategory.TYPE,
                Minecraft.getInstance().level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.MOLECULAR_ASSEMBLING_TYPE.get()));
        registration.addRecipes(
                ManaDrillRecipeCategory.TYPE,
                Minecraft.getInstance().level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.MANA_DRILL_GENERATING_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                ModItems.MOLECULAR_ASSEMBLER_9X9.get(),
                MolecularAssemblerRecipeCategory.TYPE);
        registration.addRecipeCatalyst(
                ModItems.MANA_DRILL.get(),
                ManaDrillRecipeCategory.TYPE);

        if (!ModList.get().isLoaded("avaritia")) return;
        try {
            registration.addRecipeCatalyst(ModItems.MOLECULAR_ASSEMBLER_9X9.get(),
                    SculkCraftingTableCategory.RECIPE_TYPE);
            registration.addRecipeCatalyst(ModItems.MOLECULAR_ASSEMBLER_9X9.get(),
                    NetherCraftingTableCategory.RECIPE_TYPE);
            registration.addRecipeCatalyst(ModItems.MOLECULAR_ASSEMBLER_9X9.get(),
                    EndCraftingTableCategory.RECIPE_TYPE);
            registration.addRecipeCatalyst(ModItems.MOLECULAR_ASSEMBLER_9X9.get(),
                    ExtremeCraftingTableCategory.RECIPE_TYPE);
        } catch (LinkageError | RuntimeException error) {
            MetaTechReborn.LOGGER.error(
                    "Unable to register the molecular assembler as a catalyst for Re-Avaritia tables", error);
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new ExtremePatternEncoderTransferHandler<>(
                        ModMenus.EXTREME_PATTERN_ENCODER.get(),
                        MolecularAssemblerRecipeCategory.TYPE,
                        registration.getTransferHelper(),
                        ExtremePatternEncoderRecipeGrids::fromMolecular),
                MolecularAssemblerRecipeCategory.TYPE);

        if (!ModList.get().isLoaded("avaritia")) return;
        try {
            registerAvaritiaTransfer(registration, SculkCraftingTableCategory.RECIPE_TYPE);
            registerAvaritiaTransfer(registration, NetherCraftingTableCategory.RECIPE_TYPE);
            registerAvaritiaTransfer(registration, EndCraftingTableCategory.RECIPE_TYPE);
            registerAvaritiaTransfer(registration, ExtremeCraftingTableCategory.RECIPE_TYPE);
        } catch (LinkageError | RuntimeException error) {
            MetaTechReborn.LOGGER.error(
                    "Unable to register Re-Avaritia ghost transfer for the 3x3/5x5/7x7/9x9 encoder", error);
        }
    }

    private static void registerAvaritiaTransfer(IRecipeTransferRegistration registration,
                                                  RecipeType<ITierCraftingRecipe> recipeType) {
        registration.addRecipeTransferHandler(
                new ExtremePatternEncoderTransferHandler<>(
                        ModMenus.EXTREME_PATTERN_ENCODER.get(),
                        recipeType,
                        registration.getTransferHelper(),
                        ExtremePatternEncoderRecipeGrids::fromAvaritia),
                recipeType);
    }
}
