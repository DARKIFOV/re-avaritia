package ru.rfvv.metatechreborn.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.registry.ModRecipes;

import java.util.ArrayList;
import java.util.List;

public final class ManaDrillRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient module;
    private final int manaCost;
    private final int time;
    private final List<Drop> drops;

    public ManaDrillRecipe(ResourceLocation id, Ingredient module, int manaCost, int time, List<Drop> drops) {
        this.id = id;
        this.module = module;
        this.manaCost = Math.max(0, manaCost);
        this.time = Math.max(1, time);
        this.drops = List.copyOf(drops);
    }

    public boolean matchesModule(ItemStack stack) { return module.test(stack); }
    public int manaCost() { return manaCost; }
    public int time() { return time; }
    public List<Drop> drops() { return drops; }

    public List<ItemStack> rollDrops(RandomSource random, int lootingLevel, int generationLevel) {
        List<ItemStack> result = new ArrayList<>();
        for (Drop drop : drops) {
            ItemStack stack = drop.roll(random, lootingLevel, generationLevel);
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    @Override public boolean matches(@NotNull Container container, @NotNull Level level) {
        return !container.isEmpty() && matchesModule(container.getItem(0));
    }
    @Override public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess access) {
        return getResultItem(access).copy();
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        return drops.isEmpty() ? ItemStack.EMPTY : drops.get(0).preview();
    }
    @Override public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(module);
        return ingredients;
    }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.MANA_DRILL_GENERATING_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.MANA_DRILL_GENERATING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public record Drop(ItemStack stack, int minimum, int maximum, int chance) {
        public Drop {
            minimum = Math.max(1, minimum);
            maximum = Math.max(minimum, maximum);
            chance = Math.max(0, Math.min(10_000, chance));
        }
        public ItemStack preview() {
            ItemStack result = stack.copy();
            result.setCount(minimum);
            return result;
        }
        public ItemStack roll(RandomSource random, int lootingLevel, int generationLevel) {
            int adjustedChance = Math.min(10_000, chance + Math.max(0, lootingLevel) * 350);
            if (random.nextInt(10_000) >= adjustedChance) return ItemStack.EMPTY;
            int baseCount = minimum + (maximum > minimum ? random.nextInt(maximum - minimum + 1) : 0);
            int generationMultiplier = 1 + Math.max(0, generationLevel);
            long count = (long) baseCount * generationMultiplier;
            ItemStack result = stack.copy();
            result.setCount((int) Math.min(result.getMaxStackSize(), count));
            return result;
        }
    }
}
