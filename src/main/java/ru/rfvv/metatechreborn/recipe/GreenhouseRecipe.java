package ru.rfvv.metatechreborn.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.registry.ModRecipes;

public final class GreenhouseRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient flower;
    private final Ingredient fuel;
    private final Fluid fluid;
    private final int fluidAmount;
    private final int mana;
    private final int time;
    private final boolean consumeFuel;
    private final boolean dayOnly;
    private final boolean nightOnly;

    public GreenhouseRecipe(ResourceLocation id, Ingredient flower, Ingredient fuel,
                            Fluid fluid, int fluidAmount, int mana, int time,
                            boolean consumeFuel, boolean dayOnly, boolean nightOnly) {
        this.id = id;
        this.flower = flower;
        this.fuel = fuel;
        this.fluid = fluid == null ? Fluids.EMPTY : fluid;
        this.fluidAmount = Math.max(0, fluidAmount);
        this.mana = Math.max(1, mana);
        this.time = Math.max(1, time);
        this.consumeFuel = consumeFuel;
        this.dayOnly = dayOnly;
        this.nightOnly = nightOnly;
    }

    public boolean matchesFlower(ItemStack stack) {
        return flower.test(stack);
    }

    public boolean matchesFuel(ItemStack stack) {
        return !requiresFuel() || fuel.test(stack);
    }

    public boolean requiresFuel() {
        return !fuel.isEmpty();
    }

    public boolean requiresFluid() {
        return fluid != Fluids.EMPTY && fluidAmount > 0;
    }

    public Ingredient flower() { return flower; }
    public Ingredient fuel() { return fuel; }
    public Fluid fluid() { return fluid; }
    public int fluidAmount() { return fluidAmount; }
    public int mana() { return mana; }
    public int time() { return time; }
    public boolean consumeFuel() { return consumeFuel; }
    public boolean dayOnly() { return dayOnly; }
    public boolean nightOnly() { return nightOnly; }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        if (container.isEmpty() || !matchesFlower(container.getItem(0))) return false;
        if (!requiresFuel()) return true;
        for (int slot = 1; slot < container.getContainerSize(); slot++) {
            if (matchesFuel(container.getItem(slot))) return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(flower);
        if (requiresFuel()) result.add(fuel);
        return result;
    }

    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.GREENHOUSE_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.GREENHOUSE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
}
