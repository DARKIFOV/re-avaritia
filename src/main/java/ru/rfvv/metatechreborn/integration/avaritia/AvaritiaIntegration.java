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

/**
 * This class is loaded only after Forge confirms that mod id "avaritia" exists.
 * Keeping all Re-Avaritia references here prevents optional-dependency classloading crashes.
 */
public final class AvaritiaIntegration {
    private static final int MAX_TIER = 4;

    public static Optional<MachineRecipeMatch> findMatch(Level level, IItemHandler inventory) {
        for (ITierCraftingRecipe recipe : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.CRAFTING_TABLE_RECIPE.get())) {
            Optional<TierGridView> view = createTierView(recipe, inventory);
            if (view.isPresent() && recipe.matches(view.get())) {
                return Optional.of(createMatch(recipe, view.get()));
            }
        }
        return Optional.empty();
    }

    public static Optional<MachineRecipeMatch> findMatchById(Level level, ResourceLocation id,
                                                              IItemHandler inventory) {
        return level.getRecipeManager().byKey(id)
                .filter(ITierCraftingRecipe.class::isInstance)
                .map(ITierCraftingRecipe.class::cast)
                .flatMap(recipe -> createTierView(recipe, inventory)
                        .filter(recipe::matches)
                        .map(view -> createMatch(recipe, view)));
    }

    private static Optional<TierGridView> createTierView(ITierCraftingRecipe recipe,
                                                          IItemHandler inventory) {
        int tier = recipe.getTier();
        if (tier < 1 || tier > MAX_TIER) return Optional.empty();

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
                                                    TierGridView inventory) {
        ItemStack result = recipe.assemble(inventory).copy();
        NonNullList<ItemStack> tierRemaining = recipe.getRemainingItems(inventory);
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                inventory.sourceSlots(), ItemStack.EMPTY);

        int limit = Math.min(tierRemaining.size(), inventory.getSlots());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = tierRemaining.get(slot);
            if (!stack.isEmpty()) {
                remaining.set(inventory.sourceSlot(slot), stack.copy());
            }
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

    /**
     * Presents the centered 3x3, 5x5, 7x7 or 9x9 section of MetaTech's 9x9 grid
     * as the exact inventory size expected by Re-Avaritia's tiered recipes.
     */
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

        @Override
        public int getSlots() {
            return gridSize * gridSize;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return source.getStackInSlot(sourceSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return source.insertItem(sourceSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return source.extractItem(sourceSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return source.getSlotLimit(sourceSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return source.isItemValid(sourceSlot(slot), stack);
        }
    }

    private AvaritiaIntegration() {
    }
}
