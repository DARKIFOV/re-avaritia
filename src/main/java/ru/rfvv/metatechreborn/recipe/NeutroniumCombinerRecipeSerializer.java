package ru.rfvv.metatechreborn.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NeutroniumCombinerRecipeSerializer
        implements RecipeSerializer<NeutroniumCombinerRecipe> {
    @Override
    public @NotNull NeutroniumCombinerRecipe fromJson(@NotNull ResourceLocation id,
                                                       @NotNull JsonObject json) {
        Ingredient collector = Ingredient.fromJson(GsonHelper.getNonNull(json, "collector"));
        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        int time = GsonHelper.getAsInt(json, "time", 3600);
        int energyPerTick = GsonHelper.getAsInt(json, "energy_per_tick", 250);
        return new NeutroniumCombinerRecipe(id, collector, result, time, energyPerTick);
    }

    @Override
    public @Nullable NeutroniumCombinerRecipe fromNetwork(@NotNull ResourceLocation id,
                                                           @NotNull FriendlyByteBuf buffer) {
        Ingredient collector = Ingredient.fromNetwork(buffer);
        ItemStack result = buffer.readItem();
        int time = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new NeutroniumCombinerRecipe(id, collector, result, time, energyPerTick);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer,
                          @NotNull NeutroniumCombinerRecipe recipe) {
        recipe.collector().toNetwork(buffer);
        buffer.writeItem(recipe.result());
        buffer.writeVarInt(recipe.time());
        buffer.writeVarInt(recipe.energyPerTick());
    }
}
