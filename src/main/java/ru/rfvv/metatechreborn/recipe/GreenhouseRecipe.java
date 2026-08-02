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

import java.util.Locale;

public final class GreenhouseRecipe implements Recipe<Container> {
    public enum FuelMode {
        NONE,
        INGREDIENT,
        FURNACE_FUEL,
        EDIBLE,
        WOOL_CYCLE,
        SPECIAL_FLOWER;

        public static FuelMode byName(String name) {
            if (name == null || name.isBlank()) return NONE;
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unknown greenhouse fuel_mode: " + name);
            }
        }
    }

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
    private final FuelMode fuelMode;

    public GreenhouseRecipe(ResourceLocation id, Ingredient flower, Ingredient fuel,
                            Fluid fluid, int fluidAmount, int mana, int time,
                            boolean consumeFuel, boolean dayOnly, boolean nightOnly) {
        this(id, flower, fuel, fluid, fluidAmount, mana, time, consumeFuel, dayOnly, nightOnly,
                fuel.isEmpty() ? FuelMode.NONE : FuelMode.INGREDIENT);
    }

    public GreenhouseRecipe(ResourceLocation id, Ingredient flower, Ingredient fuel,
                            Fluid fluid, int fluidAmount, int mana, int time,
                            boolean consumeFuel, boolean dayOnly, boolean nightOnly,
                            FuelMode fuelMode) {
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
        this.fuelMode = fuelMode == null ? FuelMode.NONE : fuelMode;
    }

    public boolean matchesFlower(ItemStack stack) {
        return flower.test(stack);
    }

    public boolean matchesFuel(ItemStack stack) {
        return fuelMode == FuelMode.NONE || (fuelMode == FuelMode.INGREDIENT && fuel.test(stack));
    }

    public boolean requiresFuel() {
        return fuelMode != FuelMode.NONE;
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
    public FuelMode fuelMode() { return fuelMode; }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        if (container.isEmpty() || !matchesFlower(container.getItem(0))) return false;
        if (!requiresFuel()) return true;
        for (int slot = 1; slot < container.getContainerSize(); slot++) {
            ItemStack candidate = container.getItem(slot);
            if (fuelMode == FuelMode.INGREDIENT ? fuel.test(candidate) : !candidate.isEmpty()) return true;
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
        if (!fuel.isEmpty()) result.add(fuel);
        return result;
    }

    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.GREENHOUSE_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.GREENHOUSE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
}
