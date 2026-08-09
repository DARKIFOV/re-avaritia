package ru.rfvv.metatechreborn.integration.mystical;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Generic integration for Mystical Agriculture's altar infusion recipe type. */
public final class MysticalInfusionSupport {
    public static final ResourceLocation TYPE_ID = new ResourceLocation("mysticalagriculture", "infusion");
    public static final int INPUT_SLOTS = 9;

    private MysticalInfusionSupport() {}

    public record View(ResourceLocation id, NonNullList<Ingredient> ingredients,
                       ItemStack output, Recipe<?> recipe) {
        public NonNullList<ItemStack> displayInputs() {
            NonNullList<ItemStack> result = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(INPUT_SLOTS, ingredients.size()); i++) {
                ItemStack[] stacks = ingredients.get(i).getItems();
                if (stacks.length > 0) {
                    ItemStack copy = stacks[0].copy();
                    copy.setCount(1);
                    result.set(i, copy);
                }
            }
            return result;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable RecipeType rawType() {
        return ForgeRegistries.RECIPE_TYPES.getValue(TYPE_ID);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<View> all(Level level) {
        RecipeType type = rawType();
        if (type == null) return List.of();
        List<Recipe<?>> recipes = (List) level.getRecipeManager().getAllRecipesFor(type);
        List<View> result = new ArrayList<>(recipes.size());
        for (Recipe<?> recipe : recipes) {
            View view = toView(level, recipe);
            if (view != null) result.add(view);
        }
        return List.copyOf(result);
    }

    public static Optional<View> find(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id)
                .filter(recipe -> recipe.getType() == rawType())
                .map(recipe -> toView(level, recipe));
    }

    public static Optional<View> findMatching(Level level, List<ItemStack> inputs) {
        if (inputs.size() < INPUT_SLOTS) return Optional.empty();
        for (View view : all(level)) {
            if (matches(level, view, inputs)) return Optional.of(view);
        }
        return Optional.empty();
    }

    private static @Nullable View toView(Level level, Recipe<?> recipe) {
        NonNullList<Ingredient> raw = recipe.getIngredients();
        if (raw.isEmpty() || raw.size() > INPUT_SLOTS) return null;
        NonNullList<Ingredient> ingredients = NonNullList.withSize(INPUT_SLOTS, Ingredient.EMPTY);
        for (int i = 0; i < raw.size(); i++) ingredients.set(i, raw.get(i));
        ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
        if (output.isEmpty()) return null;
        return new View(recipe.getId(), ingredients, output, recipe);
    }

    public static boolean matches(Level level, View view, List<ItemStack> inputs) {
        if (inputs.size() < INPUT_SLOTS) return false;
        return matchesContainer(level, view.recipe(), container(inputs));
    }

    public static ItemStack assemble(Level level, View view, List<ItemStack> inputs) {
        return assembleRecipe(level, view.recipe(), container(inputs));
    }

    public static NonNullList<ItemStack> remaining(Level level, View view, List<ItemStack> inputs) {
        return remainingItems(view.recipe(), container(inputs));
    }

    private static SimpleContainer container(List<ItemStack> inputs) {
        SimpleContainer container = new SimpleContainer(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) container.setItem(i, inputs.get(i).copy());
        return container;
    }

    @SuppressWarnings("unchecked")
    private static boolean matchesContainer(Level level, Recipe<?> recipe, Container container) {
        try {
            return ((Recipe<Container>) recipe).matches(container, level);
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack assembleRecipe(Level level, Recipe<?> recipe, Container container) {
        try {
            return ((Recipe<Container>) recipe).assemble(container, level.registryAccess());
        } catch (ClassCastException ignored) {
            return ItemStack.EMPTY;
        }
    }

    @SuppressWarnings("unchecked")
    private static NonNullList<ItemStack> remainingItems(Recipe<?> recipe, Container container) {
        try {
            return ((Recipe<Container>) recipe).getRemainingItems(container);
        } catch (ClassCastException ignored) {
            return NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
        }
    }
}
