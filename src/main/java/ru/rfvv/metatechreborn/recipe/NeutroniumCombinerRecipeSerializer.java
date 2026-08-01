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
        return new NeutroniumCombinerRecipe(id, collector, result,
                GsonHelper.getAsInt(json, "time", 3600),
                GsonHelper.getAsInt(json, "energy_per_tick", 250));
    }

    @Override
    public @Nullable NeutroniumCombinerRecipe fromNetwork(@NotNull ResourceLocation id,
                                                           @NotNull FriendlyByteBuf buffer) {
        return new NeutroniumCombinerRecipe(id, Ingredient.fromNetwork(buffer), buffer.readItem(),
                buffer.readVarInt(), buffer.readVarInt());
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
