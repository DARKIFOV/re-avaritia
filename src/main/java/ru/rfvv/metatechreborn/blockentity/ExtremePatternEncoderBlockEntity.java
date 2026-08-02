package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.integration.avaritia.AvaritiaIntegration;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModRecipes;

import java.util.Optional;

/** Standalone native encoder for complete 9x9 crafting patterns. */
public final class ExtremePatternEncoderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GRID_SLOTS = 81;
    public static final int BLANK_SLOT = GRID_SLOTS;
    public static final int OUTPUT_SLOT = GRID_SLOTS + 1;
    public static final int TOTAL_SLOTS = GRID_SLOTS + 2;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_ENCODED = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_NO_BLANK = 3;
    public static final int STATUS_OUTPUT_BLOCKED = 4;

    private int status = STATUS_IDLE;
    private boolean ghostGrid;
    private boolean updatingGhostGrid;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < GRID_SLOTS) {
                status = STATUS_IDLE;
                if (!updatingGhostGrid) ghostGrid = false;
            }
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < GRID_SLOTS ? 1 : slot == BLANK_SLOT ? 64 : 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < GRID_SLOTS) return !ghostGrid;
            if (slot == BLANK_SLOT) return stack.is(ModItems.BLANK_EXTREME_PATTERN.get());
            return false;
        }
    };

    private final IItemHandler recipeGrid = new IItemHandler() {
        @Override public int getSlots() { return GRID_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return ghostGrid ? stack : items.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ghostGrid ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return !ghostGrid && items.isItemValid(slot, stack);
        }
    };

    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < GRID_SLOTS ? !ghostGrid && items.isItemValid(slot, stack)
                    : items.isItemValid(slot, stack);
        }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < GRID_SLOTS && ghostGrid) return stack;
            return items.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < GRID_SLOTS && ghostGrid) return ItemStack.EMPTY;
            return items.extractItem(slot, amount, simulate);
        }
    };

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> automation);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> status;
                case 1 -> findOutput(level).isPresent() ? 1 : 0;
                case 2 -> ghostGrid ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) status = value;
            else if (index == 2) ghostGrid = value != 0;
        }
        @Override public int getCount() { return 3; }
    };

    public ExtremePatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTREME_PATTERN_ENCODER.get(), pos, state);
    }

    public boolean encode() {
        if (level == null || level.isClientSide) return false;
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            setStatus(STATUS_OUTPUT_BLOCKED);
            return false;
        }
        ItemStack blank = items.getStackInSlot(BLANK_SLOT);
        if (blank.isEmpty()) {
            setStatus(STATUS_NO_BLANK);
            return false;
        }

        Optional<ItemStack> output = findOutput(level);
        if (output.isEmpty() || output.get().isEmpty()) {
            setStatus(STATUS_NO_RECIPE);
            return false;
        }

        NonNullList<ItemStack> patternGrid = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                patternGrid.set(slot, copy);
            }
        }

        ItemStack result = output.get().copy();
        ExtremePatternData pattern = new ExtremePatternData(patternGrid, result);
        ItemStack encoded = EncodedExtremePatternItem.create(pattern);
        blank.shrink(1);
        items.setStackInSlot(BLANK_SLOT, blank);
        items.setStackInSlot(OUTPUT_SLOT, encoded);
        setStatus(STATUS_ENCODED);
        return true;
    }

    public void setGhostGrid(NonNullList<ItemStack> source, Player player) {
        if (level == null || level.isClientSide) return;
        if (!ghostGrid) returnRealGridItems(player);

        updatingGhostGrid = true;
        try {
            for (int slot = 0; slot < GRID_SLOTS; slot++) {
                ItemStack stack = slot < source.size() ? source.get(slot) : ItemStack.EMPTY;
                if (stack.isEmpty()) {
                    items.setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    items.setStackInSlot(slot, copy);
                }
            }
        } finally {
            updatingGhostGrid = false;
        }
        ghostGrid = true;
        status = STATUS_IDLE;
        setChanged();
    }

    public void clearGrid(Player player) {
        if (level == null || level.isClientSide) return;
        if (ghostGrid) {
            updatingGhostGrid = true;
            try {
                for (int slot = 0; slot < GRID_SLOTS; slot++) {
                    items.setStackInSlot(slot, ItemStack.EMPTY);
                }
            } finally {
                updatingGhostGrid = false;
            }
        } else {
            returnRealGridItems(player);
        }
        ghostGrid = false;
        setStatus(STATUS_IDLE);
    }

    private void returnRealGridItems(Player player) {
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = items.extractItem(slot, 64, false);
            if (stack.isEmpty()) continue;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }

    private Optional<ItemStack> findOutput(@Nullable Level targetLevel) {
        if (targetLevel == null) return Optional.empty();
        if (CommonConfig.ENABLE_AVARITIA_INTEGRATION.get() && ModList.get().isLoaded("avaritia")) {
            try {
                Optional<MachineRecipeMatch> match = AvaritiaIntegration.findMatch(targetLevel, recipeGrid);
                if (match.isPresent()) return Optional.of(match.get().result().copy());
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to preview Re-Avaritia 9x9 recipe in encoder", error);
            }
        }

        return targetLevel.getRecipeManager().getAllRecipesFor(ModRecipes.MOLECULAR_ASSEMBLING_TYPE.get())
                .stream()
                .filter(recipe -> recipe.matches(recipeGrid))
                .findFirst()
                .map(MolecularAssemblerRecipe::result)
                .map(ItemStack::copy);
    }

    private void setStatus(int newStatus) {
        status = newStatus;
        setChanged();
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public boolean isGhostGrid() { return ghostGrid; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = ghostGrid ? GRID_SLOTS : 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.extreme_pattern_encoder");
    }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                                 @NotNull Player player) {
        return new ExtremePatternEncoderMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Status", status);
        tag.putBoolean("GhostGrid", ghostGrid);
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        status = tag.getInt("Status");
        ghostGrid = tag.getBoolean("GhostGrid");
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
            @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }
}
