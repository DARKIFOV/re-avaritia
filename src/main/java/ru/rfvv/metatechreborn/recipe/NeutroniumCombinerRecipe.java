package ru.rfvv.metatechreborn.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.registry.ModRecipes;

public final class NeutroniumCombinerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient collector;
    private final ItemStack result;
    private final int time;
    private final int energyPerTick;

    public NeutroniumCombinerRecipe(ResourceLocation id, Ingredient collector, ItemStack result,
                                    int time, int energyPerTick) {
        this.id = id;
        this.collector = collector;
        this.result = result;
        this.time = Math.max(1, time);
        this.energyPerTick = Math.max(0, energyPerTick);
    }

    public boolean matchesCollector(ItemStack stack) { return collector.test(stack); }
    @Override public boolean matches(@NotNull SimpleContainer container, @NotNull Level level) {
        return !container.isEmpty() && matchesCollector(container.getItem(0));
    }
    @Override public @NotNull ItemStack assemble(@NotNull SimpleContainer container,
                                                  @NotNull RegistryAccess registryAccess) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) { return result.copy(); }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.NEUTRONIUM_COMBINING_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.NEUTRONIUM_COMBINING_TYPE.get(); }
    public Ingredient collector() { return collector; }
    public ItemStack result() { return result.copy(); }
    public int time() { return time; }
    public int energyPerTick() { return energyPerTick; }
}
