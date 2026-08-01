package ru.rfvv.metatechreborn.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class MolecularAssemblerRecipeSerializer implements RecipeSerializer<MolecularAssemblerRecipe> {
    @Override
    public @NotNull MolecularAssemblerRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        JsonArray patternJson = GsonHelper.getAsJsonArray(json, "pattern");
        if (patternJson.size() == 0 || patternJson.size() > 9) {
            throw new JsonSyntaxException("pattern height must be from 1 to 9");
        }

        String[] pattern = new String[patternJson.size()];
        int width = -1;
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = GsonHelper.convertToString(patternJson.get(i), "pattern[" + i + "]");
            if (pattern[i].isEmpty() || pattern[i].length() > 9) {
                throw new JsonSyntaxException("pattern width must be from 1 to 9");
            }
            if (width == -1) width = pattern[i].length();
            if (pattern[i].length() != width) {
                throw new JsonSyntaxException("all pattern rows must have the same width");
            }
        }

        Map<Character, Ingredient> key = readKey(GsonHelper.getAsJsonObject(json, "key"));
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * pattern.length, Ingredient.EMPTY);
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < width; x++) {
                char symbol = pattern[y].charAt(x);
                Ingredient ingredient = symbol == ' ' ? Ingredient.EMPTY : key.get(symbol);
                if (ingredient == null) throw new JsonSyntaxException("undefined key symbol: " + symbol);
                ingredients.set(x + y * width, ingredient);
            }
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        int time = Math.max(1, GsonHelper.getAsInt(json, "time", 400));
        int energyPerTick = Math.max(0, GsonHelper.getAsInt(json, "energy_per_tick", 500));
        return new MolecularAssemblerRecipe(id, width, pattern.length, ingredients, result, time, energyPerTick);
    }

    private static Map<Character, Ingredient> readKey(JsonObject object) {
        Map<Character, Ingredient> key = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().length() != 1 || " ".equals(entry.getKey())) {
                throw new JsonSyntaxException("key symbols must be one non-space character");
            }
            key.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
        }
        return key;
    }

    @Override
    public @Nullable MolecularAssemblerRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromNetwork(buffer));
        ItemStack result = buffer.readItem();
        int time = buffer.readVarInt();
        int energyPerTick = buffer.readVarInt();
        return new MolecularAssemblerRecipe(id, width, height, ingredients, result, time, energyPerTick);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull MolecularAssemblerRecipe recipe) {
        buffer.writeVarInt(recipe.width());
        buffer.writeVarInt(recipe.height());
        for (Ingredient ingredient : recipe.getIngredients()) ingredient.toNetwork(buffer);
        buffer.writeItem(recipe.result());
        buffer.writeVarInt(recipe.time());
        buffer.writeVarInt(recipe.energyPerTick());
    }
}
