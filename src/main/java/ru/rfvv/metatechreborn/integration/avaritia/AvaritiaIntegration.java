package ru.rfvv.metatechreborn.integration.avaritia;

import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;

import java.util.Optional;

public final class AvaritiaIntegration {
    public static Optional<MachineRecipeMatch> findMatch(Level level, IItemHandler inventory) {
        for (ITierCraftingRecipe recipe : level.getRecipeManager().getAllRecipesFor(
                ModRecipeTypes.CRAFTING_TABLE_RECIPE.get())) {
            if (recipe.getTier() <= 4 && recipe.matches(inventory)) return Optional.of(createMatch(recipe, inventory));
        }
        return Optional.empty();
    }
    public static Optional<MachineRecipeMatch> findMatchById(Level level, ResourceLocation id, IItemHandler inventory) {
        return level.getRecipeManager().byKey(id)
                .filter(ITierCraftingRecipe.class::isInstance)
                .map(ITierCraftingRecipe.class::cast)
                .filter(recipe -> recipe.getTier() <= 4 && recipe.matches(inventory))
                .map(recipe -> createMatch(recipe, inventory));
    }
    private static MachineRecipeMatch createMatch(ITierCraftingRecipe recipe, IItemHandler inventory) {
        ItemStack result = recipe.assemble(inventory).copy();
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(inventory);
        return new MachineRecipeMatch(recipe.getId(), MachineRecipeMatch.Source.AVARITIA,
                result, remaining, CommonConfig.DEFAULT_CRAFT_TIME.get(), CommonConfig.DEFAULT_ENERGY_PER_TICK.get());
    }
    private AvaritiaIntegration() {}
}
