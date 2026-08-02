package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.integration.avaritia.AvaritiaIntegration;
import ru.rfvv.metatechreborn.integration.ae2.MolecularAssemblerCraftingMachine;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.item.PatternCapacityUpgradeItem;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extreme 9x9 molecular assembler.
 *
 * Manual jobs use the visible output slot. AE2 jobs use a separate persistent
 * return buffer, so a restart, chunk unload or temporarily full ME network cannot
 * duplicate or delete the crafted result.
 */
public final class MolecularAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GRID_SIZE = 9;
    public static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;
    public static final int OUTPUT_SLOT = GRID_SLOTS;
    public static final int ENERGY_SLOT = OUTPUT_SLOT + 1;
    public static final int TOTAL_SLOTS = ENERGY_SLOT + 1;

    public static final int BASE_PATTERN_SLOTS = 9;
    public static final int MAX_PATTERN_SLOTS = 28;
    /** Keeps old 36-slot development saves recoverable by breaking the block. */
    public static final int PATTERN_STORAGE_SLOTS = 36;
    public static final int EXTRA_PATTERN_SLOTS = MAX_PATTERN_SLOTS - BASE_PATTERN_SLOTS;
    private static final int NETWORK_RETURN_SLOTS = 18;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_RUNNING = 4;
    public static final int STATUS_AE2_READY = 5;
    public static final int STATUS_RETURNING_TO_NETWORK = 6;

    private boolean suppressInventoryCallbacks;
    private int progress;
    private int maxProgress;
    private int status = STATUS_IDLE;
    private ResourceLocation lockedRecipeId;
    private MachineRecipeMatch.Source lockedRecipeSource;
    private ItemStack lockedResult = ItemStack.EMPTY;
    private boolean templateLocked;
    private boolean ae2JobActive;

    private final NonNullList<ItemStack> lockedTemplate =
            NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!suppressInventoryCallbacks && slot < GRID_SLOTS) progress = 0;
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < GRID_SLOTS || slot == ENERGY_SLOT) return 1;
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            if (slot == ENERGY_SLOT) return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            return canInsertIntoGridSlot(slot, stack);
        }
    };

    private final ItemStackHandler patternItems = new ItemStackHandler(PATTERN_STORAGE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            requestAe2PatternUpdate();
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < getActivePatternSlots()
                    && stack.getItem() instanceof EncodedExtremePatternItem
                    && EncodedExtremePatternItem.read(stack).isPresent();
        }
    };

    private final ItemStackHandler patternUpgradeItems = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            requestAe2PatternUpdate();
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof PatternCapacityUpgradeItem;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canRemovePatternUpgrade()) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    };

    private final ItemStackHandler networkReturnBuffer = new ItemStackHandler(NETWORK_RETURN_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final IItemHandler recipeGrid = new IItemHandler() {
        @Override public int getSlots() { return GRID_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return items.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    /** External automation may insert ingredients, but never extract the recipe grid. */
    private final IItemHandler externalItems = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == OUTPUT_SLOT) return stack;
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == OUTPUT_SLOT || slot == ENERGY_SLOT) return items.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.ASSEMBLER_CAPACITY.get(),
            CommonConfig.ASSEMBLER_MAX_RECEIVE.get(),
            this::setChanged
    );

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                case 4 -> templateLocked ? 1 : 0;
                case 5 -> getActivePatternSlots();
                case 6 -> getInstalledPatternCount();
                case 7 -> status;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 7) status = value;
        }

        @Override public int getCount() { return 8; }
    };

    public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOLECULAR_ASSEMBLER_9X9.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  MolecularAssemblerBlockEntity blockEntity) {
        blockEntity.tickServer(level);
    }

    private void tickServer(Level level) {
        chargeFromEnergyItem();
        flushNetworkReturnBuffer();

        if (hasPendingNetworkReturns()) {
            setStatus(STATUS_RETURNING_TO_NETWORK);
            return;
        }

        if (CommonConfig.AUTO_EJECT_OUTPUT.get() && level.getGameTime() % 5L == 0L) {
            autoEjectOutput(level);
        }

        Optional<MachineRecipeMatch> match = lockedRecipeId == null
                ? findAnyMatch(level)
                : findLockedMatch(level);

        if (match.isEmpty() || match.get().result().isEmpty()) {
            resetProgressOnly();
            setStatus(isGridEmpty() && getInstalledPatternCount() > 0 ? STATUS_AE2_READY
                    : isGridEmpty() ? STATUS_IDLE : STATUS_NO_RECIPE);
            return;
        }

        MachineRecipeMatch active = match.get();
        if (ae2JobActive && !lockedResult.isEmpty()
                && !ItemStack.isSameItemSameTags(active.result(), lockedResult)) {
            resetProgressOnly();
            setStatus(STATUS_NO_RECIPE);
            return;
        }
        if (lockedRecipeId == null) lockRecipe(active);

        maxProgress = Math.max(1, active.craftTime());
        if (!ae2JobActive && !canOutput(active.result())) {
            setStatus(STATUS_OUTPUT_FULL);
            return;
        }
        if (ae2JobActive && !canQueueNetworkOutputs(active)) {
            setStatus(STATUS_OUTPUT_FULL);
            return;
        }

        int energyPerTick = Math.max(0, active.energyPerTick());
        if (energy.getEnergyStored() < energyPerTick) {
            setStatus(STATUS_NO_ENERGY);
            return;
        }

        setStatus(STATUS_RUNNING);
        if (energyPerTick > 0) energy.extractEnergy(energyPerTick, false);
        progress++;
        setChanged();

        if (progress >= maxProgress) {
            Optional<MachineRecipeMatch> validated = findLockedMatch(level);
            if (validated.isPresent()
                    && (!ae2JobActive && canOutput(validated.get().result())
                    || ae2JobActive && canQueueNetworkOutputs(validated.get()))) {
                completeCraft(level, validated.get());
            } else {
                progress = 0;
                setStatus(STATUS_NO_RECIPE);
                setChanged();
            }
        }
    }

    private void chargeFromEnergyItem() {
        ItemStack stack = items.getStackInSlot(ENERGY_SLOT);
        if (stack.isEmpty() || energy.getEnergyStored() >= energy.getMaxEnergyStored()) return;
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            int request = Math.min(CommonConfig.ASSEMBLER_MAX_RECEIVE.get(),
                    energy.getMaxEnergyStored() - energy.getEnergyStored());
            int available = source.extractEnergy(request, true);
            int accepted = energy.receiveEnergy(available, false);
            if (accepted > 0) source.extractEnergy(accepted, false);
        });
    }

    private Optional<MachineRecipeMatch> findAnyMatch(Level level) {
        if (isAvaritiaAvailable()) {
            try {
                Optional<MachineRecipeMatch> avaritia = AvaritiaIntegration.findMatch(level, recipeGrid);
                if (avaritia.isPresent()) return avaritia;
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Re-Avaritia recipe integration failed; using MetaTech recipes", error);
            }
        }

        return level.getRecipeManager().getAllRecipesFor(ModRecipes.MOLECULAR_ASSEMBLING_TYPE.get())
                .stream()
                .filter(recipe -> recipe.matches(recipeGrid))
                .findFirst()
                .map(this::createMetaTechMatch);
    }

    private Optional<MachineRecipeMatch> findLockedMatch(Level level) {
        if (lockedRecipeId == null || lockedRecipeSource == null) return Optional.empty();

        if (lockedRecipeSource == MachineRecipeMatch.Source.AVARITIA) {
            if (!isAvaritiaAvailable()) return Optional.empty();
            try {
                return AvaritiaIntegration.findMatchById(level, lockedRecipeId, recipeGrid);
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to resolve locked Re-Avaritia recipe {}", lockedRecipeId, error);
                return Optional.empty();
            }
        }

        return level.getRecipeManager().byKey(lockedRecipeId)
                .filter(MolecularAssemblerRecipe.class::isInstance)
                .map(MolecularAssemblerRecipe.class::cast)
                .filter(recipe -> recipe.matches(recipeGrid))
                .map(this::createMetaTechMatch);
    }

    private MachineRecipeMatch createMetaTechMatch(MolecularAssemblerRecipe recipe) {
        return new MachineRecipeMatch(
                recipe.getId(),
                MachineRecipeMatch.Source.METATECH,
                recipe.result(),
                recipe.getRemainingItems(recipeGrid),
                recipe.time(),
                recipe.energyPerTick()
        );
    }

    private boolean isAvaritiaAvailable() {
        return CommonConfig.ENABLE_AVARITIA_INTEGRATION.get()
                && ModList.get().isLoaded("avaritia");
    }

    private void lockRecipe(MachineRecipeMatch match) {
        lockedRecipeId = match.id();
        lockedRecipeSource = match.source();
        if (lockedResult.isEmpty()) lockedResult = match.result().copy();
        if (!templateLocked) copyGridToLockedTemplate();
        templateLocked = true;
        setChanged();
    }

    private void copyGridToLockedTemplate() {
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                lockedTemplate.set(slot, ItemStack.EMPTY);
            } else {
                ItemStack template = stack.copy();
                template.setCount(1);
                lockedTemplate.set(slot, template);
            }
        }
    }

    public void clearRecipeLock() {
        if (isAe2Busy()) return;
        clearRecipeLockInternal();
    }

    private void clearRecipeLockInternal() {
        lockedRecipeId = null;
        lockedRecipeSource = null;
        lockedResult = ItemStack.EMPTY;
        templateLocked = false;
        progress = 0;
        maxProgress = 0;
        for (int slot = 0; slot < GRID_SLOTS; slot++) lockedTemplate.set(slot, ItemStack.EMPTY);
        setStatus(STATUS_IDLE);
        setChanged();
    }

    public int getActivePatternSlots() {
        return patternUpgradeItems.getStackInSlot(0).getItem() instanceof PatternCapacityUpgradeItem
                ? MAX_PATTERN_SLOTS : BASE_PATTERN_SLOTS;
    }

    public int getInstalledPatternCount() {
        int count = 0;
        for (int slot = 0; slot < getActivePatternSlots(); slot++) {
            if (EncodedExtremePatternItem.read(patternItems.getStackInSlot(slot)).isPresent()) count++;
        }
        return count;
    }

    public boolean hasOccupiedExtraPatternSlots() {
        for (int slot = BASE_PATTERN_SLOTS; slot < PATTERN_STORAGE_SLOTS; slot++) {
            if (!patternItems.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    public boolean canRemovePatternUpgrade() {
        return !hasOccupiedExtraPatternSlots();
    }

    public boolean canAcceptAe2Plan() {
        return progress == 0 && !ae2JobActive && isGridEmpty() && !hasPendingNetworkReturns();
    }

    public boolean isAe2Busy() {
        return ae2JobActive || hasPendingNetworkReturns();
    }

    private boolean isGridEmpty() {
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    public ItemStack getLockedResultForAutomation() {
        return lockedResult.copy();
    }

    /** Receives an already validated, complete AE2 input placement atomically. */
    public boolean acceptDecodedAe2Pattern(ExtremePatternData pattern,
                                           NonNullList<ItemStack> placement) {
        if (!canAcceptAe2Plan() || placement.size() != GRID_SLOTS) return false;
        if (pattern.output().isEmpty()) return false;

        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack expected = pattern.inputs().get(slot);
            ItemStack supplied = placement.get(slot);
            if (expected.isEmpty() != supplied.isEmpty()) return false;
            if (!expected.isEmpty() && !ItemStack.isSameItemSameTags(expected, supplied)) return false;
        }

        applyPlacement(placement);
        lockedRecipeId = null;
        lockedRecipeSource = null;
        lockedResult = pattern.output().copy();
        templateLocked = true;
        ae2JobActive = true;
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            lockedTemplate.set(slot, pattern.inputs().get(slot).copy());
        }
        setStatus(STATUS_RUNNING);
        setChanged();
        return true;
    }

    public boolean acceptExternalPatternBatch(List<ItemStack> suppliedStacks,
                                              ItemStack requestedOutput,
                                              long requestedAmount) {
        if (!canAcceptAe2Plan() || requestedOutput.isEmpty()) return false;

        for (int slot = 0; slot < getActivePatternSlots(); slot++) {
            Optional<ExtremePatternData> decoded = EncodedExtremePatternItem.read(patternItems.getStackInSlot(slot));
            if (decoded.isEmpty()) continue;
            ExtremePatternData pattern = decoded.get();
            if (!ItemStack.isSameItemSameTags(pattern.output(), requestedOutput)
                    || requestedAmount < pattern.output().getCount()) continue;

            Optional<NonNullList<ItemStack>> placement = createPlacement(pattern.inputs(), suppliedStacks);
            if (placement.isEmpty()) continue;
            return acceptDecodedAe2Pattern(pattern, placement.get());
        }
        return false;
    }

    public boolean acceptExternalPatternBatch(List<ItemStack> suppliedStacks) {
        if (!canAcceptAe2Plan() || !templateLocked || lockedResult.isEmpty()) return false;
        Optional<NonNullList<ItemStack>> placement = createPlacement(lockedTemplate, suppliedStacks);
        if (placement.isEmpty()) return false;
        ExtremePatternData pattern = new ExtremePatternData(copyLockedTemplate(), lockedResult);
        return acceptDecodedAe2Pattern(pattern, placement.get());
    }

    private NonNullList<ItemStack> copyLockedTemplate() {
        NonNullList<ItemStack> copy = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) copy.set(slot, lockedTemplate.get(slot).copy());
        return copy;
    }

    private Optional<NonNullList<ItemStack>> createPlacement(List<ItemStack> template,
                                                              List<ItemStack> suppliedStacks) {
        List<ItemStack> supplied = new ArrayList<>(suppliedStacks.size());
        for (ItemStack stack : suppliedStacks) {
            if (!stack.isEmpty()) supplied.add(stack.copy());
        }

        NonNullList<ItemStack> placement = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack expected = template.get(slot);
            if (expected.isEmpty()) continue;

            int matchIndex = -1;
            for (int index = 0; index < supplied.size(); index++) {
                ItemStack candidate = supplied.get(index);
                if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(expected, candidate)) {
                    matchIndex = index;
                    break;
                }
            }
            if (matchIndex < 0) return Optional.empty();

            ItemStack candidate = supplied.get(matchIndex);
            ItemStack placed = candidate.copy();
            placed.setCount(1);
            placement.set(slot, placed);
            candidate.shrink(1);
        }

        for (ItemStack remaining : supplied) {
            if (!remaining.isEmpty()) return Optional.empty();
        }
        return Optional.of(placement);
    }

    private void applyPlacement(NonNullList<ItemStack> placement) {
        suppressInventoryCallbacks = true;
        try {
            for (int slot = 0; slot < GRID_SLOTS; slot++) items.setStackInSlot(slot, placement.get(slot).copy());
        } finally {
            suppressInventoryCallbacks = false;
        }
        progress = 0;
        setChanged();
    }

    private boolean canInsertIntoGridSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SLOTS) return false;
        if (!templateLocked) return !ae2JobActive;
        ItemStack template = lockedTemplate.get(slot);
        return !template.isEmpty() && ItemStack.isSameItemSameTags(template, stack);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return result.getCount() <= result.getMaxStackSize();
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean canQueueNetworkOutputs(MachineRecipeMatch match) {
        ItemStackHandler simulated = copyNetworkReturnBuffer();
        for (ItemStack remainder : match.remainingItems()) {
            if (!insertIntoHandler(simulated, remainder.copy(), false).isEmpty()) return false;
        }
        return insertIntoHandler(simulated, match.result().copy(), false).isEmpty();
    }

    private ItemStackHandler copyNetworkReturnBuffer() {
        ItemStackHandler copy = new ItemStackHandler(NETWORK_RETURN_SLOTS);
        for (int slot = 0; slot < NETWORK_RETURN_SLOTS; slot++) {
            copy.setStackInSlot(slot, networkReturnBuffer.getStackInSlot(slot).copy());
        }
        return copy;
    }

    private void completeCraft(Level level, MachineRecipeMatch match) {
        suppressInventoryCallbacks = true;
        try {
            for (int slot = 0; slot < GRID_SLOTS; slot++) {
                if (!items.getStackInSlot(slot).isEmpty()) items.extractItem(slot, 1, false);
            }

            NonNullList<ItemStack> remaining = match.remainingItems();
            if (ae2JobActive) {
                for (ItemStack remainder : remaining) {
                    if (!remainder.isEmpty()) insertIntoHandler(networkReturnBuffer, remainder.copy(), false);
                }
                insertIntoHandler(networkReturnBuffer, match.result().copy(), false);
            } else {
                for (int slot = 0; slot < Math.min(GRID_SLOTS, remaining.size()); slot++) {
                    ItemStack remainder = remaining.get(slot).copy();
                    if (remainder.isEmpty()) continue;
                    ItemStack current = items.getStackInSlot(slot);
                    if (current.isEmpty()) {
                        items.setStackInSlot(slot, remainder);
                    } else {
                        ItemStack notInserted = insertIntoOutput(remainder);
                        if (!notInserted.isEmpty()) popItem(level, worldPosition, notInserted);
                    }
                }

                ItemStack notInserted = insertIntoOutput(match.result().copy());
                if (!notInserted.isEmpty()) popItem(level, worldPosition, notInserted);
            }
        } finally {
            suppressInventoryCallbacks = false;
        }

        progress = 0;
        maxProgress = match.craftTime();
        if (ae2JobActive) {
            setStatus(STATUS_RETURNING_TO_NETWORK);
        } else {
            setStatus(getInstalledPatternCount() > 0 ? STATUS_AE2_READY : STATUS_IDLE);
        }
        setChanged();
    }

    private static ItemStack insertIntoHandler(ItemStackHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private ItemStack insertIntoOutput(ItemStack stack) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.setStackInSlot(OUTPUT_SLOT, stack.copy());
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameTags(output, stack)) return stack;

        int transferable = Math.min(stack.getCount(), output.getMaxStackSize() - output.getCount());
        if (transferable <= 0) return stack;
        output.grow(transferable);
        items.setStackInSlot(OUTPUT_SLOT, output);
        ItemStack remainder = stack.copy();
        remainder.shrink(transferable);
        return remainder;
    }

    private void flushNetworkReturnBuffer() {
        if (!ModList.get().isLoaded("ae2")) return;
        boolean changed = false;
        for (int slot = 0; slot < NETWORK_RETURN_SLOTS; slot++) {
            ItemStack stack = networkReturnBuffer.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int inserted;
            try {
                inserted = MolecularAssemblerCraftingMachine.insertIntoNetwork(this, stack);
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Failed returning molecular assembler output to AE2", error);
                return;
            }
            if (inserted > 0) {
                networkReturnBuffer.extractItem(slot, inserted, false);
                changed = true;
            }
        }

        if (!hasPendingNetworkReturns() && ae2JobActive) {
            ae2JobActive = false;
            clearRecipeLockInternal();
            setStatus(getInstalledPatternCount() > 0 ? STATUS_AE2_READY : STATUS_IDLE);
            changed = true;
        }
        if (changed) setChanged();
    }

    private boolean hasPendingNetworkReturns() {
        for (int slot = 0; slot < NETWORK_RETURN_SLOTS; slot++) {
            if (!networkReturnBuffer.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private void autoEjectOutput(Level level) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;

            Optional<IItemHandler> target = neighbour
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target.get(), output.copy(), false);
            if (remainder.getCount() != output.getCount()) {
                suppressInventoryCallbacks = true;
                items.setStackInSlot(OUTPUT_SLOT, remainder);
                suppressInventoryCallbacks = false;
                setChanged();
                output = remainder;
                if (output.isEmpty()) return;
            }
        }
    }

    private void requestAe2PatternUpdate() {
        if (level == null || level.isClientSide || !ModList.get().isLoaded("ae2")) return;
        try {
            MolecularAssemblerCraftingMachine.requestPatternUpdate(this);
        } catch (LinkageError | RuntimeException error) {
            MetaTechReborn.LOGGER.error("Unable to refresh AE2 molecular assembler patterns", error);
        }
    }

    private void setStatus(int newStatus) {
        if (status != newStatus) {
            status = newStatus;
            setChanged();
        }
    }

    private void resetProgressOnly() {
        if (progress != 0 || maxProgress != 0) {
            progress = 0;
            maxProgress = 0;
            setChanged();
        }
    }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        for (int slot = 0; slot < PATTERN_STORAGE_SLOTS; slot++) {
            ItemStack stack = patternItems.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        ItemStack upgrade = patternUpgradeItems.getStackInSlot(0);
        if (!upgrade.isEmpty()) drops.add(upgrade.copy());
        for (int slot = 0; slot < NETWORK_RETURN_SLOTS; slot++) {
            ItemStack stack = networkReturnBuffer.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    public ItemStackHandler getItems() { return items; }
    public ItemStackHandler getPatternItems() { return patternItems; }
    public ItemStackHandler getPatternUpgradeItems() { return patternUpgradeItems; }
    public ContainerData getData() { return data; }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.molecular_assembler_9x9");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory,
                                                       @NotNull Player player) {
        return new MolecularAssemblerMenu(containerId, inventory, this, data);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (ModList.get().isLoaded("ae2")) {
            try {
                MolecularAssemblerCraftingMachine.clearRemoved(this);
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to initialize AE2 node for molecular assembler", error);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (ModList.get().isLoaded("ae2")) {
            try {
                MolecularAssemblerCraftingMachine.remove(this);
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to remove AE2 node for molecular assembler", error);
            }
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Patterns", patternItems.serializeNBT());
        tag.put("PatternUpgrade", patternUpgradeItems.serializeNBT());
        tag.put("NetworkReturns", networkReturnBuffer.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Status", status);
        tag.putBoolean("TemplateLocked", templateLocked);
        tag.putBoolean("Ae2JobActive", ae2JobActive);

        if (lockedRecipeId != null && lockedRecipeSource != null) {
            tag.putString("LockedRecipe", lockedRecipeId.toString());
            tag.putString("LockedSource", lockedRecipeSource.name());
        }
        if (!lockedResult.isEmpty()) tag.put("LockedResult", lockedResult.save(new CompoundTag()));

        ListTag templateTag = new ListTag();
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = lockedTemplate.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Stack", stack.save(new CompoundTag()));
            templateTag.add(entry);
        }
        tag.put("LockedTemplate", templateTag);

        if (ModList.get().isLoaded("ae2")) {
            try {
                CompoundTag nodeTag = new CompoundTag();
                MolecularAssemblerCraftingMachine.saveNode(this, nodeTag);
                if (!nodeTag.isEmpty()) tag.put("Ae2Node", nodeTag);
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to save molecular assembler AE2 node", error);
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Patterns", Tag.TAG_COMPOUND)) patternItems.deserializeNBT(tag.getCompound("Patterns"));
        if (tag.contains("PatternUpgrade", Tag.TAG_COMPOUND)) {
            patternUpgradeItems.deserializeNBT(tag.getCompound("PatternUpgrade"));
        }
        if (tag.contains("NetworkReturns", Tag.TAG_COMPOUND)) {
            networkReturnBuffer.deserializeNBT(tag.getCompound("NetworkReturns"));
        }
        energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        status = tag.getInt("Status");
        templateLocked = tag.getBoolean("TemplateLocked");
        ae2JobActive = tag.getBoolean("Ae2JobActive");

        lockedRecipeId = null;
        lockedRecipeSource = null;
        lockedResult = tag.contains("LockedResult", Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("LockedResult")) : ItemStack.EMPTY;
        if (tag.contains("LockedRecipe", Tag.TAG_STRING) && tag.contains("LockedSource", Tag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("LockedRecipe"));
            try {
                MachineRecipeMatch.Source source = MachineRecipeMatch.Source.valueOf(tag.getString("LockedSource"));
                lockedRecipeId = parsed;
                lockedRecipeSource = parsed == null ? null : source;
            } catch (IllegalArgumentException ignored) {
                lockedRecipeId = null;
                lockedRecipeSource = null;
            }
        }

        for (int slot = 0; slot < GRID_SLOTS; slot++) lockedTemplate.set(slot, ItemStack.EMPTY);
        ListTag templateTag = tag.getList("LockedTemplate", Tag.TAG_COMPOUND);
        for (int index = 0; index < templateTag.size(); index++) {
            CompoundTag entry = templateTag.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            if (slot < GRID_SLOTS && entry.contains("Stack", Tag.TAG_COMPOUND)) {
                lockedTemplate.set(slot, ItemStack.of(entry.getCompound("Stack")));
            }
        }

        if (ModList.get().isLoaded("ae2") && tag.contains("Ae2Node", Tag.TAG_COMPOUND)) {
            try {
                MolecularAssemblerCraftingMachine.loadNode(this, tag.getCompound("Ae2Node"));
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to load molecular assembler AE2 node", error);
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        if (ModList.get().isLoaded("ae2")) {
            try {
                LazyOptional<T> ae2Capability = MolecularAssemblerCraftingMachine.getCapability(this, cap);
                if (ae2Capability != null) return ae2Capability;
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Unable to expose molecular assembler AE2 capability", error);
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }

    private static void popItem(Level level, BlockPos pos, ItemStack stack) {
        net.minecraft.world.Containers.dropItemStack(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
    }
}
