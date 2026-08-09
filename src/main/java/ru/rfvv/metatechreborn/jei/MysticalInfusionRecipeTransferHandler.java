package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.integration.mystical.MysticalInfusionSupport;
import ru.rfvv.metatechreborn.menu.MysticalInfusionEncoderMenu;
import ru.rfvv.metatechreborn.network.ModNetwork;
import ru.rfvv.metatechreborn.network.MysticalInfusionRecipePacket;

import java.util.Optional;

public final class MysticalInfusionRecipeTransferHandler
        implements IRecipeTransferHandler<MysticalInfusionEncoderMenu, MysticalInfusionSupport.View> {
    private final MenuType<MysticalInfusionEncoderMenu> menuType;
    private final IRecipeTransferHandlerHelper helper;

    public MysticalInfusionRecipeTransferHandler(MenuType<MysticalInfusionEncoderMenu> menuType,
                                                  IRecipeTransferHandlerHelper helper) {
        this.menuType = menuType; this.helper = helper;
    }
    @Override public @NotNull Class<? extends MysticalInfusionEncoderMenu> getContainerClass() {
        return MysticalInfusionEncoderMenu.class;
    }
    @Override public @NotNull Optional<MenuType<MysticalInfusionEncoderMenu>> getMenuType() { return Optional.of(menuType); }
    @Override public @NotNull RecipeType<MysticalInfusionSupport.View> getRecipeType() { return MysticalInfusionRecipeCategory.TYPE; }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(@NotNull MysticalInfusionEncoderMenu menu,
                                                          @NotNull MysticalInfusionSupport.View recipe,
                                                          @NotNull IRecipeSlotsView slots,
                                                          @NotNull Player player,
                                                          boolean maxTransfer, boolean doTransfer) {
        if (recipe.output().isEmpty()) return helper.createInternalError();
        if (doTransfer) ModNetwork.CHANNEL.sendToServer(new MysticalInfusionRecipePacket(recipe.id()));
        return null;
    }
}
