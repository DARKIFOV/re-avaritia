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
import ru.rfvv.metatechreborn.item.NeutroniumCombinerUpgradeItem;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;
import ru.rfvv.metatechreborn.recipe.NeutroniumCombinerRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Forge 1.20.1 rewrite of the MetaAdvanced neutron combiner.
 * Nine collector modules run independently and are never consumed.
 */
public final class NeutroniumCombinerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 40;
    public static final int FIRST_OUTPUT_SLOT = INPUT_SLOTS;
    public static final int UPGRADE_SLOTS = 4;
    public static final int FIRST_UPGRADE_SLOT = FIRST_OUTPUT_SLOT + OUTPUT_SLOTS;
    public static final int TOTAL_SLOTS = FIRST_UPGRADE_SLOT + UPGRADE_SLOTS;

    private final int[] progresses = new int[INPUT_SLOTS];
    private final int[] maxProgresses = new int[INPUT_SLOTS];

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < INPUT_SLOTS) {
                progresses[slot] = 0;
                maxProgresses[slot] = 0;
            } else if (slot >= FIRST_UPGRADE_SLOT) {
                Arrays.fill(progresses, 0);
                Arrays.fill(maxProgresses, 0);
            }
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < INPUT_SLOTS) return 1;
            if (slot >= FIRST_UPGRADE_SLOT) return 8;
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < INPUT_SLOTS) return isCollector(stack);
            if (slot >= FIRST_UPGRADE_SLOT) {
                return stack.getItem() instanceof NeutroniumCombinerUpgradeItem;
            }
            return false;
        }
    };

    private final IItemHandler externalItems = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < INPUT_SLOTS || slot >= FIRST_UPGRADE_SLOT) {
                return items.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= FIRST_OUTPUT_SLOT && slot < FIRST_UPGRADE_SLOT
                    ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.NEUTRON_COMBINER_CAPACITY.get(),
            CommonConfig.NEUTRON_COMBINER_MAX_RECEIVE.get(),
            this::setChanged
    );

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < INPUT_SLOTS) return progresses[index];
            if (index >= INPUT_SLOTS && index < INPUT_SLOTS * 2) {
                return maxProgresses[index - INPUT_SLOTS];
            }
            return switch (index) {
                case 18 -> energy.getEnergyStored();
                case 19 -> energy.getMaxEnergyStored();
                case 20 -> getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.SPEED);
                case 21 -> getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.EFFICIENCY);
                case 22 -> getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.OUTPUT);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < INPUT_SLOTS) progresses[index] = value;
            if (index >= INPUT_SLOTS && index < INPUT_SLOTS * 2) {
                maxProgresses[index - INPUT_SLOTS] = value;
            }
        }

        @Override public int getCount() { return 23; }
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

        int speed = getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.SPEED);
        int efficiency = getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.EFFICIENCY);
        int output = getUpgradeCount(NeutroniumCombinerUpgradeItem.Type.OUTPUT);

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack collector = items.getStackInSlot(slot);
            Optional<NeutroniumCombinerRecipe> match = recipes.stream()
                    .filter(recipe -> recipe.matchesCollector(collector))
                    .findFirst();

            if (match.isEmpty()) {
                resetProgress(slot);
                continue;
            }

            NeutroniumCombinerRecipe recipe = match.get();
            int operationTime = adjustedTime(recipe.time(), speed);
            int energyPerTick = adjustedEnergy(recipe.energyPerTick(), efficiency, output);
            ItemStack result = recipe.result();
            result.setCount(Math.max(1, result.getCount() * (1 + output)));

            maxProgresses[slot] = operationTime;
            if (!canInsertOutput(result)) continue;
            if (energy.getEnergyStored() < energyPerTick) continue;

            if (energyPerTick > 0) energy.extractEnergy(energyPerTick, false);
            progresses[slot]++;
            setChanged();

            if (progresses[slot] >= operationTime) {
                ItemStack remainder = insertOutput(result);
                if (remainder.isEmpty()) {
                    progresses[slot] = 0;
                    setChanged();
                }
            }
        }

        if (CommonConfig.NEUTRON_COMBINER_AUTO_EJECT.get() && level.getGameTime() % 5L == 0L) {
            autoEject(level);
        }
    }

    private static int adjustedTime(int baseTime, int speed) {
        int denominator = 100 + speed * 25;
        return Math.max(5, (Math.max(1, baseTime) * 100 + denominator - 1) / denominator);
    }

    private static int adjustedEnergy(int baseEnergy, int efficiency, int output) {
        if (baseEnergy <= 0) return 0;
        int efficiencyPercent = Math.max(20, 100 - efficiency * 10);
        int outputPercent = 100 + output * 50;
        long adjusted = (long) baseEnergy * efficiencyPercent * outputPercent;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, (adjusted + 9_999L) / 10_000L));
    }

    private int getUpgradeCount(NeutroniumCombinerUpgradeItem.Type type) {
        int count = 0;
        for (int slot = FIRST_UPGRADE_SLOT; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.getItem() instanceof NeutroniumCombinerUpgradeItem upgrade && upgrade.type() == type) {
                count += stack.getCount();
            }
        }
        return Math.min(type.maximum(), count);
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

    private boolean canInsertOutput(ItemStack stack) {
        return insertOutput(stack, true).isEmpty();
    }

    private ItemStack insertOutput(ItemStack stack) {
        return insertOutput(stack, false);
    }

    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_UPGRADE_SLOT && !remainder.isEmpty(); slot++) {
            remainder = items.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private void autoEject(Level level) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            Optional<IItemHandler> target = neighbour
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;

            for (int slot = FIRST_OUTPUT_SLOT; slot < FIRST_UPGRADE_SLOT; slot++) {
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

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.neutronium_combiner");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory,
                                                       @NotNull Player player) {
        return new NeutroniumCombinerMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putIntArray("Progresses", progresses);
        tag.putIntArray("MaxProgresses", maxProgresses);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        energy.setEnergyStored(tag.getInt("Energy"));
        copyArray(tag.getIntArray("Progresses"), progresses);
        copyArray(tag.getIntArray("MaxProgresses"), maxProgresses);
    }

    private static void copyArray(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap,
            @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        energyCapability.invalidate();
    }
}
