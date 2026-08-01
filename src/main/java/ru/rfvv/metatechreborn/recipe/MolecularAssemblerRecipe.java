package ru.rfvv.metatechreborn.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModRecipes;

public final class MolecularAssemblerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int time;
    private final int energyPerTick;

    public MolecularAssemblerRecipe(ResourceLocation id, int width, int height,
            NonNullList<Ingredient> ingredients, ItemStack result, int time, int energyPerTick) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.time = time;
        this.energyPerTick = energyPerTick;
    }

    @Override public boolean matches(@NotNull SimpleContainer container, @NotNull Level level) { return false; }
    public boolean matches(IItemHandler handler) {
        if (handler.getSlots() < MolecularAssemblerBlockEntity.GRID_SLOTS) return false;
        NonNullList<Ingredient> grid = getGridIngredients();
        for (int slot = 0; slot < MolecularAssemblerBlockEntity.GRID_SLOTS; slot++) {
            if (!grid.get(slot).test(handler.getStackInSlot(slot))) return false;
        }
        return true;
    }
    public NonNullList<Ingredient> getGridIngredients() {
        NonNullList<Ingredient> grid = NonNullList.withSize(MolecularAssemblerBlockEntity.GRID_SLOTS, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            grid.set(x + y * 9, ingredients.get(x + y * width));
        }
        return grid;
    }
    public NonNullList<ItemStack> getRemainingItems(IItemHandler handler) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(MolecularAssemblerBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.hasCraftingRemainingItem()) remaining.set(i, stack.getCraftingRemainingItem());
        }
        return remaining;
    }
    @Override public @NotNull ItemStack assemble(@NotNull SimpleContainer container, @NotNull RegistryAccess access) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width >= this.width && height >= this.height; }
    @Override public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) { return result.copy(); }
    @Override public @NotNull NonNullList<Ingredient> getIngredients() { return ingredients; }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.MOLECULAR_ASSEMBLING_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.MOLECULAR_ASSEMBLING_TYPE.get(); }
    public int width() { return width; }
    public int height() { return height; }
    public int time() { return time; }
    public int energyPerTick() { return energyPerTick; }
    public ItemStack result() { return result.copy(); }
}
