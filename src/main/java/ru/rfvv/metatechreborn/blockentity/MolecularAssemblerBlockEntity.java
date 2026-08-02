package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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

public final class MolecularAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GRID_SIZE = 9;
    public static final int GRID_SLOTS = GRID_SIZE * GRID_SIZE;
    public static final int OUTPUT_SLOT = GRID_SLOTS;
    public static final int ENERGY_SLOT = OUTPUT_SLOT + 1;
    public static final int TOTAL_SLOTS = ENERGY_SLOT + 1;

    public static final int BASE_PATTERN_SLOTS = 9;
    public static final int MAX_PATTERN_SLOTS = 36;
    public static final int EXTRA_PATTERN_SLOTS = MAX_PATTERN_SLOTS - BASE_PATTERN_SLOTS;
    public static final int AE2_SPEED_CARD_SLOTS = 4;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_RUNNING = 4;
    public static final int STATUS_AE2_READY = 5;

    private boolean suppressInventoryCallbacks;
    private int progress;
    private int maxProgress;
    private int status = STATUS_IDLE;
    private ResourceLocation lockedRecipeId;
    private MachineRecipeMatch.Source lockedRecipeSource;
    private ItemStack lockedResult = ItemStack.EMPTY;
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

    private final ItemStackHandler patternItems = new ItemStackHandler(MAX_PATTERN_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < getActivePatternSlots()
                    && stack.getItem() instanceof EncodedExtremePatternItem
                    && EncodedExtremePatternItem.read(stack).isPresent();
        }
    };

    private final ItemStackHandler patternUpgradeItems = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof PatternCapacityUpgradeItem;
        }
    };

    private final ItemStackHandler ae2SpeedCards = new ItemStackHandler(AE2_SPEED_CARD_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            progress = 0;
            maxProgress = 0;
            setChanged();
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isAe2SpeedCard(stack);
        }
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
                case 4 -> lockedRecipeId == null ? 0 : 1;
                case 5 -> getActivePatternSlots();
                case 6 -> getInstalledPatternCount();
                case 7 -> status;
                case 8 -> getAe2SpeedCardCount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 7) status = value;
        }

        @Override public int getCount() { return 9; }
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
        if (lockedRecipeId == null) lockRecipe(active);

        maxProgress = adjustedCraftTime(active.craftTime());
        if (!canOutput(active.result())) {
            setStatus(STATUS_OUTPUT_FULL);
            return;
        }

        int energyPerTick = adjustedEnergyPerTick(active.energyPerTick());
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
            if (validated.isPresent() && canOutput(validated.get().result())) {
                completeCraft(level, validated.get());
            } else {
                progress = 0;
                setStatus(STATUS_NO_RECIPE);
                setChanged();
            }
        }
    }

    private int adjustedCraftTime(int baseTime) {
        int multiplier = 1 + getAe2SpeedCardCount();
        return Math.max(1, (Math.max(1, baseTime) + multiplier - 1) / multiplier);
    }

    private int adjustedEnergyPerTick(int baseEnergy) {
        if (baseEnergy <= 0) return 0;
        long adjusted = (long) baseEnergy * (1 + getAe2SpeedCardCount());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, adjusted));
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
        lockedResult = match.result().copy();
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
        setChanged();
    }

    public void clearRecipeLock() {
        lockedRecipeId = null;
        lockedRecipeSource = null;
        lockedResult = ItemStack.EMPTY;
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

    public int getAe2SpeedCardCount() {
        int count = 0;
        for (int slot = 0; slot < AE2_SPEED_CARD_SLOTS; slot++) {
            if (isAe2SpeedCard(ae2SpeedCards.getStackInSlot(slot))) count++;
        }
        return count;
    }

    public static boolean isAe2SpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "ae2".equals(id.getNamespace()) && "speed_card".equals(id.getPath());
    }

    public boolean canAcceptAe2Plan() {
        if (progress != 0 || !isGridEmpty()) return false;
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || output.getCount() < output.getMaxStackSize();
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

    public boolean acceptExternalPatternBatch(List<ItemStack> suppliedStacks,
                                              ItemStack requestedOutput,
                                              long requestedAmount) {
        if (!canAcceptAe2Plan() || requestedOutput.isEmpty()) return false;

        for (int slot = 0; slot < getActivePatternSlots(); slot++) {
            Optional<ExtremePatternData> decoded = EncodedExtremePatternItem.read(patternItems.getStackInSlot(slot));
            if (decoded.isEmpty()) continue;
            ExtremePatternData pattern = decoded.get();
            if (!ItemStack.isSameItemSameTags(pattern.output(), requestedOutput)
                    || requestedAmount < pattern.output().getCount()
                    || !canOutput(pattern.output())) continue;

            Optional<NonNullList<ItemStack>> placement = createPatternPlacement(pattern, suppliedStacks);
            if (placement.isEmpty()) continue;

            clearRecipeLock();
            applyPlacement(placement.get());
            lockedResult = pattern.output().copy();
            for (int gridSlot = 0; gridSlot < GRID_SLOTS; gridSlot++) {
                lockedTemplate.set(gridSlot, pattern.inputs().get(gridSlot).copy());
            }
            setStatus(STATUS_RUNNING);
            setChanged();
            return true;
        }

        if (lockedRecipeId != null && !lockedResult.isEmpty()
                && ItemStack.isSameItemSameTags(lockedResult, requestedOutput)
                && requestedAmount >= lockedResult.getCount()
                && canOutput(lockedResult)) {
            Optional<NonNullList<ItemStack>> placement = createLockedPlacement(suppliedStacks);
            if (placement.isPresent()) {
                applyPlacement(placement.get());
                return true;
            }
        }
        return false;
    }

    public boolean acceptExternalPatternBatch(List<ItemStack> suppliedStacks) {
        if (!canAcceptAe2Plan() || lockedRecipeId == null || lockedResult.isEmpty() || !canOutput(lockedResult)) {
            return false;
        }
        Optional<NonNullList<ItemStack>> placement = createLockedPlacement(suppliedStacks);
        if (placement.isEmpty()) return false;
        applyPlacement(placement.get());
        return true;
    }

    private Optional<NonNullList<ItemStack>> createPatternPlacement(ExtremePatternData pattern,
                                                                    List<ItemStack> suppliedStacks) {
        return createPlacement(pattern.inputs(), suppliedStacks);
    }

    private Optional<NonNullList<ItemStack>> createLockedPlacement(List<ItemStack> suppliedStacks) {
        return createPlacement(lockedTemplate, suppliedStacks);
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
            for (int slot = 0; slot < GRID_SLOTS; slot++) items.setStackInSlot(slot, placement.get(slot));
        } finally {
            suppressInventoryCallbacks = false;
        }
        progress = 0;
        setChanged();
    }

    private boolean canInsertIntoGridSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SLOTS) return false;
        if (lockedRecipeId == null) return true;
        ItemStack template = lockedTemplate.get(slot);
        return !template.isEmpty() && ItemStack.isSameItemSameTags(template, stack);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return result.getCount() <= result.getMaxStackSize();
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeCraft(Level level, MachineRecipeMatch match) {
        suppressInventoryCallbacks = true;
        try {
            for (int slot = 0; slot < GRID_SLOTS; slot++) {
                if (!items.getStackInSlot(slot).isEmpty()) items.extractItem(slot, 1, false);
            }

            NonNullList<ItemStack> remaining = match.remainingItems();
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
        } finally {
            suppressInventoryCallbacks = false;
        }

        progress = 0;
        maxProgress = adjustedCraftTime(match.craftTime());
        setStatus(getInstalledPatternCount() > 0 ? STATUS_AE2_READY : STATUS_IDLE);
        setChanged();
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
        for (int slot = 0; slot < MAX_PATTERN_SLOTS; slot++) {
            ItemStack stack = patternItems.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        ItemStack upgrade = patternUpgradeItems.getStackInSlot(0);
        if (!upgrade.isEmpty()) drops.add(upgrade.copy());
        for (int slot = 0; slot < AE2_SPEED_CARD_SLOTS; slot++) {
            ItemStack card = ae2SpeedCards.getStackInSlot(slot);
            if (!card.isEmpty()) drops.add(card.copy());
        }
        return drops;
    }

    public ItemStackHandler getItems() { return items; }
    public ItemStackHandler getPatternItems() { return patternItems; }
    public ItemStackHandler getPatternUpgradeItems() { return patternUpgradeItems; }
    public ItemStackHandler getAe2SpeedCards() { return ae2SpeedCards; }
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Patterns", patternItems.serializeNBT());
        tag.put("PatternUpgrade", patternUpgradeItems.serializeNBT());
        tag.put("Ae2SpeedCards", ae2SpeedCards.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Status", status);

        if (lockedRecipeId != null && lockedRecipeSource != null) {
            tag.putString("LockedRecipe", lockedRecipeId.toString());
            tag.putString("LockedSource", lockedRecipeSource.name());
            if (!lockedResult.isEmpty()) tag.put("LockedResult", lockedResult.save(new CompoundTag()));
        }

        ListTag templateTag = new ListTag();
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = lockedTemplate.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            stack.save(entry);
            templateTag.add(entry);
        }
        tag.put("LockedTemplate", templateTag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Patterns", Tag.TAG_COMPOUND)) patternItems.deserializeNBT(tag.getCompound("Patterns"));
        if (tag.contains("PatternUpgrade", Tag.TAG_COMPOUND)) {
            patternUpgradeItems.deserializeNBT(tag.getCompound("PatternUpgrade"));
        }
        if (tag.contains("Ae2SpeedCards", Tag.TAG_COMPOUND)) {
            ae2SpeedCards.deserializeNBT(tag.getCompound("Ae2SpeedCards"));
        }
        energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        status = tag.getInt("Status");

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
            if (slot < GRID_SLOTS) lockedTemplate.set(slot, ItemStack.of(entry));
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        if (ModList.get().isLoaded("ae2")) {
            LazyOptional<T> ae2Capability = MolecularAssemblerCraftingMachine.getCapability(this, cap);
            if (ae2Capability != null) return ae2Capability;
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
        if (ModList.get().isLoaded("ae2")) MolecularAssemblerCraftingMachine.invalidate(this);
    }

    private static void popItem(Level level, BlockPos pos, ItemStack stack) {
        net.minecraft.world.Containers.dropItemStack(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
    }
}
