package ru.rfvv.metatechreborn.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GreenhouseRecipeSerializer implements RecipeSerializer<GreenhouseRecipe> {
    @Override
    public @NotNull GreenhouseRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        Ingredient flower = Ingredient.fromJson(json.get("flower"));
        Ingredient fuel = json.has("fuel") ? Ingredient.fromJson(json.get("fuel")) : Ingredient.EMPTY;
        Fluid fluid = Fluids.EMPTY;
        int fluidAmount = 0;
        if (json.has("fluid")) {
            ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
            fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid == Fluids.EMPTY && !fluidId.equals(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY))) {
                throw new IllegalArgumentException("Unknown greenhouse fluid: " + fluidId);
            }
            fluidAmount = GsonHelper.getAsInt(json, "fluid_amount", 1000);
        }
        int mana = GsonHelper.getAsInt(json, "mana", 1000);
        int time = GsonHelper.getAsInt(json, "time", 200);
        boolean consumeFuel = GsonHelper.getAsBoolean(json, "consume_fuel", !fuel.isEmpty());
        boolean dayOnly = GsonHelper.getAsBoolean(json, "day_only", false);
        boolean nightOnly = GsonHelper.getAsBoolean(json, "night_only", false);
        if (dayOnly && nightOnly) throw new IllegalArgumentException("Greenhouse recipe cannot be day-only and night-only");
        return new GreenhouseRecipe(id, flower, fuel, fluid, fluidAmount, mana, time,
                consumeFuel, dayOnly, nightOnly);
    }

    @Override
    public @Nullable GreenhouseRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
        Ingredient flower = Ingredient.fromNetwork(buffer);
        Ingredient fuel = Ingredient.fromNetwork(buffer);
        Fluid fluid = BuiltInRegistries.FLUID.get(buffer.readResourceLocation());
        int fluidAmount = buffer.readVarInt();
        int mana = buffer.readVarInt();
        int time = buffer.readVarInt();
        boolean consumeFuel = buffer.readBoolean();
        boolean dayOnly = buffer.readBoolean();
        boolean nightOnly = buffer.readBoolean();
        return new GreenhouseRecipe(id, flower, fuel, fluid, fluidAmount, mana, time,
                consumeFuel, dayOnly, nightOnly);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull GreenhouseRecipe recipe) {
        recipe.flower().toNetwork(buffer);
        recipe.fuel().toNetwork(buffer);
        buffer.writeResourceLocation(BuiltInRegistries.FLUID.getKey(recipe.fluid()));
        buffer.writeVarInt(recipe.fluidAmount());
        buffer.writeVarInt(recipe.mana());
        buffer.writeVarInt(recipe.time());
        buffer.writeBoolean(recipe.consumeFuel());
        buffer.writeBoolean(recipe.dayOnly());
        buffer.writeBoolean(recipe.nightOnly());
    }
}
