package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.integration.mystical.MysticalInfusionSupport;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

@JeiPlugin
public final class MysticalInfusionJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(MetaTechReborn.MOD_ID, "mystical_infusion_jei");
    @Override public @NotNull ResourceLocation getPluginUid() { return UID; }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MysticalInfusionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        registration.addRecipes(MysticalInfusionRecipeCategory.TYPE,
                MysticalInfusionSupport.all(Minecraft.getInstance().level));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModMysticalInfusion.ASSEMBLER_ITEM.get(), MysticalInfusionRecipeCategory.TYPE);
        registration.addRecipeCatalyst(ModMysticalInfusion.ENCODER_ITEM.get(), MysticalInfusionRecipeCategory.TYPE);
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new MysticalInfusionRecipeTransferHandler(
                ModMysticalInfusion.ENCODER_MENU.get(), registration.getTransferHelper()),
                MysticalInfusionRecipeCategory.TYPE);
    }
}
