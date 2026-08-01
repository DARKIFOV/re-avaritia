package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;
import ru.rfvv.metatechreborn.recipe.NeutroniumCombinerRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.List;
import java.util.Optional;

public final class NeutroniumCombinerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 40;
    public static final int FIRST_OUTPUT_SLOT = INPUT_SLOTS;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    private final int[] progresses = new int[INPUT_SLOTS];
    private final int[] maxProgresses = new int[INPUT_SLOTS];

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            if (slot < INPUT_SLOTS) {
                progresses[slot] = 0;
                maxProgresses[slot] = 0;
            }
            setChanged();
        }
        @Override public int getSlotLimit(int slot) { return slot < INPUT_SLOTS ? 1 : 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= FIRST_OUTPUT_SLOT || isCollector(stack);
        }
    };

    private final IItemHandler externalItems = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot < INPUT_SLOTS ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= FIRST_OUTPUT_SLOT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.NEUTRON_COMBINER_CAPACITY.get(),
            CommonConfig.NEUTRON_COMBINER_MAX_RECEIVE.get(), this::setChanged);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            if (index >= 0 && index < INPUT_SLOTS) return progresses[index];
            if (index >= INPUT_SLOTS && index < INPUT_SLOTS * 2) return maxProgresses[index - INPUT_SLOTS];
            return index == 18 ? energy.getEnergyStored() : index == 19 ? energy.getMaxEnergyStored() : 0;
        }
        @Override public void set(int index, int value) {
            if (index >= 0 && index < INPUT_SLOTS) progresses[index] = value;
            else if (index >= INPUT_SLOTS && index < INPUT_SLOTS * 2) maxProgresses[index - INPUT_SLOTS] = value;
        }
        @Override public int getCount() { return 20; }
    };

    public NeutroniumCombinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEUTRONIUM_COMBINER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  NeutroniumCombinerBlockEntity blockEntity) {
        blockEntity.tickServer(level);
    }

    private void tickServer(Level level) {
        List<NeutroniumCombinerRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.NEUTRONIUM_COMBINING_TYPE.get());
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack collector = items.getStackInSlot(slot);
            Optional<NeutroniumCombinerRecipe> match = recipes.stream()
                    .filter(recipe -> recipe.matchesCollector(collector)).findFirst();
            if (match.isEmpty()) {
                resetProgress(slot);
                continue;
            }
            NeutroniumCombinerRecipe recipe = match.get();
            maxProgresses[slot] = recipe.time();
            if (!canInsertOutput(recipe.result()) || energy.getEnergyStored() < recipe.energyPerTick()) continue;
            if (recipe.energyPerTick() > 0) energy.extractEnergy(recipe.energyPerTick(), false);
            progresses[slot]++;
            setChanged();
            if (progresses[slot] >= recipe.time() && insertOutput(recipe.result(), false).isEmpty()) {
                progresses[slot] = 0;
                setChanged();
            }
        }
        if (CommonConfig.NEUTRON_COMBINER_AUTO_EJECT.get() && level.getGameTime() % 5L == 0L) autoEject(level);
    }

    private boolean isCollector(ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.NEUTRONIUM_COMBINING_TYPE.get())
                .stream().anyMatch(recipe -> recipe.matchesCollector(stack));
    }

    private void resetProgress(int slot) {
        if (progresses[slot] != 0 || maxProgresses[slot] != 0) {
            progresses[slot] = 0;
            maxProgresses[slot] = 0;
            setChanged();
        }
    }

    private boolean canInsertOutput(ItemStack stack) { return insertOutput(stack, true).isEmpty(); }
    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = FIRST_OUTPUT_SLOT; slot < TOTAL_SLOTS && !remainder.isEmpty(); slot++) {
            remainder = items.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private void autoEject(Level level) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            Optional<IItemHandler> target = neighbour.getCapability(
                    ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;
            for (int slot = FIRST_OUTPUT_SLOT; slot < TOTAL_SLOTS; slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target.get(), stack.copy(), false);
                if (remainder.getCount() != stack.getCount()) items.setStackInSlot(slot, remainder);
            }
        }
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.neutronium_combiner");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                                 @NotNull Player player) {
        return new NeutroniumCombinerMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putIntArray("Progresses", progresses);
        tag.putIntArray("MaxProgresses", maxProgresses);
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        energy.setEnergyStored(tag.getInt("Energy"));
        copyArray(tag.getIntArray("Progresses"), progresses);
        copyArray(tag.getIntArray("MaxProgresses"), maxProgresses);
    }
    private static void copyArray(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }
}
