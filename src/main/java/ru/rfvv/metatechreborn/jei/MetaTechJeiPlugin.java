package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.registry.ModItems;
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
                new NeutroniumCombinerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
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
                NeutroniumCombinerRecipeCategory.TYPE,
                Minecraft.getInstance().level.getRecipeManager()
                        .getAllRecipesFor(ModRecipes.NEUTRONIUM_COMBINING_TYPE.get()));
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
                ModItems.NEUTRONIUM_COMBINER.get(),
                NeutroniumCombinerRecipeCategory.TYPE);
        registration.addRecipeCatalyst(
                ModItems.MANA_DRILL.get(),
                ManaDrillRecipeCategory.TYPE);
    }
}
