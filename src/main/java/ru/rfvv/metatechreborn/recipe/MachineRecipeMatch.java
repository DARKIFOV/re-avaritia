package ru.rfvv.metatechreborn.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record MachineRecipeMatch(
        ResourceLocation id,
        Source source,
        ItemStack result,
        NonNullList<ItemStack> remainingItems,
        int craftTime,
        int energyPerTick
) {
    public enum Source {
        METATECH,
        AVARITIA
    }
}
