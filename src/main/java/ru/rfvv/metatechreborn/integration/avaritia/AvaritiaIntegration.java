package ru.rfvv.metatechreborn.integration.avaritia;

import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;

import java.util.Optional;
import java.util.function.IntUnaryOperator;

/** Optional Re-Avaritia recipe integration. */
public final class AvaritiaIntegration {
    private static final int MAX_TIER = 4;

    public static Optional<MachineRecipeMatch> findMatch(Level level, IItemHandler inventory) {
        for (ITierCraftingRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.CRAFTING_TABLE_RECIPE.get())) {
            Optional<MachineRecipeMatch> match = matchRecipe(recipe, inventory);
            if (match.isPresent()) return match;
        }
        return Optional.empty();
    }

    public static Optional<MachineRecipeMatch> findMatchById(Level level, ResourceLocation id,
                                                              IItemHandler inventory) {
        return level.getRecipeManager().byKey(id)
                .filter(ITierCraftingRecipe.class::isInstance)
                .map(ITierCraftingRecipe.class::cast)
                .flatMap(recipe -> matchRecipe(recipe, inventory));
    }

    private static Optional<MachineRecipeMatch> matchRecipe(ITierCraftingRecipe recipe,
                                                             IItemHandler inventory) {
        /*
         * Preserve Re-Avaritia's native behaviour first. This is essential for tier-4
         * 9x9 recipes: they must see the real 81-slot handler, exactly as they did
         * before support for smaller tables was added.
         */
        if (recipe.matches(inventory)) {
            return Optional.of(createMatch(recipe, inventory, inventory.getSlots(), slot -> slot));
        }

        Optional<TierGridView> view = createSmallerTierView(recipe, inventory);
        if (view.isPresent() && recipe.matches(view.get())) {
            TierGridView grid = view.get();
            return Optional.of(createMatch(recipe, grid, grid.sourceSlots(), grid::sourceSlot));
        }
        return Optional.empty();
    }

    private static Optional<TierGridView> createSmallerTierView(ITierCraftingRecipe recipe,
                                                                 IItemHandler inventory) {
        int tier = recipe.getTier();
        if (tier < 1 || tier >= MAX_TIER) return Optional.empty();

        int gridSize = tier * 2 + 1;
        int sourceSlots = inventory.getSlots();
        int sourceWidth = (int) Math.sqrt(sourceSlots);
        if (sourceWidth * sourceWidth != sourceSlots || sourceWidth < gridSize) {
            return Optional.empty();
        }

        int offset = (sourceWidth - gridSize) / 2;
        if (hasItemsOutsideActiveGrid(inventory, sourceWidth, gridSize, offset)) {
            return Optional.empty();
        }
        return Optional.of(new TierGridView(inventory, sourceWidth, gridSize, offset));
    }

    private static boolean hasItemsOutsideActiveGrid(IItemHandler inventory, int sourceWidth,
                                                      int gridSize, int offset) {
        int max = offset + gridSize;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            int x = slot % sourceWidth;
            int y = slot / sourceWidth;
            boolean active = x >= offset && x < max && y >= offset && y < max;
            if (!active && !inventory.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static MachineRecipeMatch createMatch(ITierCraftingRecipe recipe,
                                                   IItemHandler recipeInventory,
                                                   int sourceSlots,
                                                   IntUnaryOperator sourceSlot) {
        ItemStack result = recipe.assemble(recipeInventory).copy();
        NonNullList<ItemStack> recipeRemaining = recipe.getRemainingItems(recipeInventory);
        NonNullList<ItemStack> remaining = NonNullList.withSize(sourceSlots, ItemStack.EMPTY);

        int limit = Math.min(recipeRemaining.size(), recipeInventory.getSlots());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = recipeRemaining.get(slot);
            if (!stack.isEmpty()) remaining.set(sourceSlot.applyAsInt(slot), stack.copy());
        }

        return new MachineRecipeMatch(
                recipe.getId(),
                MachineRecipeMatch.Source.AVARITIA,
                result,
                remaining,
                CommonConfig.DEFAULT_CRAFT_TIME.get(),
                CommonConfig.DEFAULT_ENERGY_PER_TICK.get()
        );
    }

    /** Centered 3x3, 5x5 or 7x7 view over MetaTech's 9x9 grid. */
    private static final class TierGridView implements IItemHandler {
        private final IItemHandler source;
        private final int sourceWidth;
        private final int gridSize;
        private final int offset;

        private TierGridView(IItemHandler source, int sourceWidth, int gridSize, int offset) {
            this.source = source;
            this.sourceWidth = sourceWidth;
            this.gridSize = gridSize;
            this.offset = offset;
        }

        private int sourceSlots() {
            return source.getSlots();
        }

        private int sourceSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new IndexOutOfBoundsException("Tier grid slot " + slot + " outside " + getSlots());
            }
            int x = slot % gridSize;
            int y = slot / gridSize;
            return offset + x + (offset + y) * sourceWidth;
        }

        @Override public int getSlots() { return gridSize * gridSize; }
        @Override public ItemStack getStackInSlot(int slot) { return source.getStackInSlot(sourceSlot(slot)); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return source.insertItem(sourceSlot(slot), stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return source.extractItem(sourceSlot(slot), amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return source.getSlotLimit(sourceSlot(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return source.isItemValid(sourceSlot(slot), stack);
        }
    }

    private AvaritiaIntegration() {}
}
