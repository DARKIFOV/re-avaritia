package ru.rfvv.metatechreborn.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;
import ru.rfvv.metatechreborn.network.EncoderGhostRecipePacket;
import ru.rfvv.metatechreborn.network.ModNetwork;

import java.util.Optional;

public final class ExtremePatternEncoderTransferHandler<R>
        implements IRecipeTransferHandler<ExtremePatternEncoderMenu, R> {

    @FunctionalInterface
    public interface GridFactory<R> {
        NonNullList<ItemStack> create(R recipe);
    }

    private final MenuType<ExtremePatternEncoderMenu> menuType;
    private final RecipeType<R> recipeType;
    private final IRecipeTransferHandlerHelper helper;
    private final GridFactory<R> gridFactory;

    public ExtremePatternEncoderTransferHandler(MenuType<ExtremePatternEncoderMenu> menuType,
                                                RecipeType<R> recipeType,
                                                IRecipeTransferHandlerHelper helper,
                                                GridFactory<R> gridFactory) {
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.helper = helper;
        this.gridFactory = gridFactory;
    }

    @Override
    public @NotNull Class<? extends ExtremePatternEncoderMenu> getContainerClass() {
        return ExtremePatternEncoderMenu.class;
    }

    @Override
    public @NotNull Optional<MenuType<ExtremePatternEncoderMenu>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public @NotNull RecipeType<R> getRecipeType() {
        return recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            @NotNull ExtremePatternEncoderMenu menu,
            @NotNull R recipe,
            @NotNull IRecipeSlotsView recipeSlots,
            @NotNull Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        final NonNullList<ItemStack> grid;
        try {
            grid = gridFactory.create(recipe);
        } catch (RuntimeException error) {
            return helper.createInternalError();
        }

        if (grid.size() != ExtremePatternEncoderBlockEntity.GRID_SLOTS
                || grid.stream().allMatch(ItemStack::isEmpty)) {
            return helper.createUserErrorWithTooltip(
                    Component.translatable("gui.metatech_reborn.encoder.no_recipe"));
        }

        if (doTransfer) {
            ModNetwork.CHANNEL.sendToServer(new EncoderGhostRecipePacket(grid));
        }
        return null;
    }
}
