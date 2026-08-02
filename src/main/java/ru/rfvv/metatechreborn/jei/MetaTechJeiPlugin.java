package ru.rfvv.metatechreborn.jei;

import committee.nova.mods.avaritia.init.compat.jei.category.tables.ExtremeCraftingTableCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
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
            registration.addRecipeTransferHandler(
                    new ExtremePatternEncoderTransferHandler<>(
                            ModMenus.EXTREME_PATTERN_ENCODER.get(),
                            ExtremeCraftingTableCategory.RECIPE_TYPE,
                            registration.getTransferHelper(),
                            ExtremePatternEncoderRecipeGrids::fromAvaritia),
                    ExtremeCraftingTableCategory.RECIPE_TYPE);
        } catch (LinkageError | RuntimeException error) {
            MetaTechReborn.LOGGER.error("Unable to register Re-Avaritia ghost transfer for the 9x9 encoder", error);
        }
    }
}
