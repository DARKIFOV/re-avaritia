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
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;
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
    public static final int TOTAL_SLOTS = GRID_SLOTS + 1;

    private boolean suppressInventoryCallbacks;
    private int progress;
    private int maxProgress;
    private ResourceLocation lockedRecipeId;
    private MachineRecipeMatch.Source lockedRecipeSource;
    private ItemStack lockedResult = ItemStack.EMPTY;
    private final NonNullList<ItemStack> lockedTemplate =
            NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!suppressInventoryCallbacks && slot < GRID_SLOTS) {
                progress = 0;
            }
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < GRID_SLOTS ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            return canInsertIntoGridSlot(slot, stack);
        }
    };

    private final IItemHandler recipeGrid = new IItemHandler() {
        @Override
        public int getSlots() {
            return GRID_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    private final IItemHandler externalItems = new IItemHandler() {
        @Override
        public int getSlots() {
            return TOTAL_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == OUTPUT_SLOT) return stack;
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
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
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 1) maxProgress = value;
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOLECULAR_ASSEMBLER_9X9.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  MolecularAssemblerBlockEntity blockEntity) {
        blockEntity.tickServer(level);
    }

    private void tickServer(Level level) {
        if (CommonConfig.AUTO_EJECT_OUTPUT.get() && level.getGameTime() % 5L == 0L) {
            autoEjectOutput(level);
        }

        Optional<MachineRecipeMatch> match = lockedRecipeId == null
                ? findAnyMatch(level)
                : findLockedMatch(level);

        if (match.isEmpty() || match.get().result().isEmpty()) {
            if (progress != 0 || maxProgress != 0) {
                progress = 0;
                maxProgress = 0;
                setChanged();
            }
            return;
        }

        MachineRecipeMatch active = match.get();
        if (lockedRecipeId == null) {
            lockRecipe(active);
        }

        maxProgress = Math.max(1, active.craftTime());
        if (!canOutput(active.result())) return;

        int energyPerTick = Math.max(0, active.energyPerTick());
        if (energy.getEnergyStored() < energyPerTick) return;

        if (energyPerTick > 0) energy.extractEnergy(energyPerTick, false);
        progress++;
        setChanged();

        if (progress >= maxProgress) {
            Optional<MachineRecipeMatch> validated = findLockedMatch(level);
            if (validated.isPresent() && canOutput(validated.get().result())) {
                completeCraft(level, validated.get());
            } else {
                progress = 0;
                setChanged();
            }
        }
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
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            lockedTemplate.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    public boolean canAcceptAe2Plan() {
        if (lockedRecipeId == null || lockedResult.isEmpty() || progress != 0) return false;
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) return false;
        }
        return canOutput(lockedResult);
    }

    public ItemStack getLockedResultForAutomation() {
        return lockedResult.copy();
    }

    /** Atomically places one complete locked-recipe batch supplied by an AE2 pattern provider. */
    public boolean acceptExternalPatternBatch(List<ItemStack> suppliedStacks) {
        if (!canAcceptAe2Plan()) return false;

        List<ItemStack> supplied = new ArrayList<>(suppliedStacks.size());
        for (ItemStack stack : suppliedStacks) {
            if (!stack.isEmpty()) supplied.add(stack.copy());
        }

        NonNullList<ItemStack> placement = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack template = lockedTemplate.get(slot);
            if (template.isEmpty()) continue;

            int matchIndex = -1;
            for (int i = 0; i < supplied.size(); i++) {
                ItemStack candidate = supplied.get(i);
                if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(template, candidate)) {
                    matchIndex = i;
                    break;
                }
            }
            if (matchIndex < 0) return false;

            ItemStack candidate = supplied.get(matchIndex);
            ItemStack placed = candidate.copy();
            placed.setCount(1);
            placement.set(slot, placed);
            candidate.shrink(1);
        }

        for (ItemStack remaining : supplied) {
            if (!remaining.isEmpty()) return false;
        }

        suppressInventoryCallbacks = true;
        try {
            for (int slot = 0; slot < GRID_SLOTS; slot++) {
                items.setStackInSlot(slot, placement.get(slot));
            }
        } finally {
            suppressInventoryCallbacks = false;
        }
        progress = 0;
        setChanged();
        return true;
    }

    private boolean canInsertIntoGridSlot(int slot, ItemStack stack) {
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
                if (!items.getStackInSlot(slot).isEmpty()) {
                    items.extractItem(slot, 1, false);
                }
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
                    if (!notInserted.isEmpty()) {
                        popItem(level, worldPosition, notInserted);
                    }
                }
            }

            ItemStack notInserted = insertIntoOutput(match.result().copy());
            if (!notInserted.isEmpty()) {
                popItem(level, worldPosition, notInserted);
            }
        } finally {
            suppressInventoryCallbacks = false;
        }

        progress = 0;
        maxProgress = match.craftTime();
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
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .resolve();
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

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public @NotNull Component getDisplayName() {
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
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);

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
        energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");

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
        for (int i = 0; i < templateTag.size(); i++) {
            CompoundTag entry = templateTag.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < GRID_SLOTS) lockedTemplate.set(slot, ItemStack.of(entry));
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
                                                       @Nullable Direction side) {
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
