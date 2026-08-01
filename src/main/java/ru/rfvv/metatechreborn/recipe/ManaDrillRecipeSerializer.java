package ru.rfvv.metatechreborn.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ManaDrillRecipeSerializer implements RecipeSerializer<ManaDrillRecipe> {
    @Override
    public @NotNull ManaDrillRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        Ingredient module = Ingredient.fromJson(json.get("module"));
        int manaCost = GsonHelper.getAsInt(json, "mana_cost", 10_000);
        int time = GsonHelper.getAsInt(json, "time", 200);
        JsonArray dropArray = GsonHelper.getAsJsonArray(json, "drops");
        List<ManaDrillRecipe.Drop> drops = new ArrayList<>();
        for (int index = 0; index < dropArray.size(); index++) {
            JsonObject dropJson = GsonHelper.convertToJsonObject(dropArray.get(index), "drops[" + index + "]");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(dropJson, "item"));
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) throw new IllegalArgumentException("Unknown mana drill drop item: " + itemId);
            int minimum = GsonHelper.getAsInt(dropJson, "min", 1);
            int maximum = GsonHelper.getAsInt(dropJson, "max", minimum);
            int chance = GsonHelper.getAsInt(dropJson, "chance", 10_000);
            drops.add(new ManaDrillRecipe.Drop(new ItemStack(item), minimum, maximum, chance));
        }
        if (drops.isEmpty()) throw new IllegalArgumentException("Mana drill recipe must contain at least one drop");
        return new ManaDrillRecipe(id, module, manaCost, time, drops);
    }

    @Override
    public @Nullable ManaDrillRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
        Ingredient module = Ingredient.fromNetwork(buffer);
        int manaCost = buffer.readVarInt();
        int time = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<ManaDrillRecipe.Drop> drops = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ItemStack stack = buffer.readItem();
            int minimum = buffer.readVarInt();
            int maximum = buffer.readVarInt();
            int chance = buffer.readVarInt();
            drops.add(new ManaDrillRecipe.Drop(stack, minimum, maximum, chance));
        }
        return new ManaDrillRecipe(id, module, manaCost, time, drops);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ManaDrillRecipe recipe) {
        recipe.getIngredients().get(0).toNetwork(buffer);
        buffer.writeVarInt(recipe.manaCost());
        buffer.writeVarInt(recipe.time());
        buffer.writeVarInt(recipe.drops().size());
        for (ManaDrillRecipe.Drop drop : recipe.drops()) {
            buffer.writeItem(drop.stack());
            buffer.writeVarInt(drop.minimum());
            buffer.writeVarInt(drop.maximum());
            buffer.writeVarInt(drop.chance());
        }
    }
}
