package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import ru.rfvv.metatechreborn.item.LuckConverterUpgradeItem;
import ru.rfvv.metatechreborn.item.LuckModuleItem;
import ru.rfvv.metatechreborn.menu.LuckConverterMenu;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Modern Forge rewrite of MetaAdvanced's normal and advanced luck changers. */
public final class LuckConverterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_INPUTS = 72;
    public static final int FIRST_OUTPUT = 72;
    public static final int MAX_OUTPUTS = 60;
    public static final int MODULE_SLOT = 132;
    public static final int FIRST_UPGRADE = 133;
    public static final int UPGRADE_SLOTS = 6;
    public static final int ENERGY_SLOT = 139;
    public static final int TOTAL_SLOTS = 140;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_NO_MODULE = 2;
    public static final int STATUS_NO_ENERGY = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_VALID_INPUT = 5;

    private int progress;
    private int maxProgress;
    private int status;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == MODULE_SLOT || slot == ENERGY_SLOT) return 1;
            if (slot >= FIRST_UPGRADE && slot < ENERGY_SLOT) return 8;
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < MAX_INPUTS) return stack.getItem() instanceof BlockItem;
            if (slot >= FIRST_OUTPUT && slot < MODULE_SLOT) return false;
            if (slot == MODULE_SLOT) {
                return stack.getItem() instanceof LuckModuleItem || stack.getItem() instanceof PickaxeItem;
            }
            if (slot >= FIRST_UPGRADE && slot < ENERGY_SLOT) {
                return stack.getItem() instanceof LuckConverterUpgradeItem;
            }
            return slot == ENERGY_SLOT && stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        }
    };

    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < inputSlots() || slot == MODULE_SLOT
                    || slot >= FIRST_UPGRADE && slot <= ENERGY_SLOT) {
                return items.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= FIRST_OUTPUT && slot < FIRST_OUTPUT + outputSlots()) {
                return items.extractItem(slot, amount, simulate);
            }
            if (slot == ENERGY_SLOT) return items.extractItem(slot, amount, simulate);
            return ItemStack.EMPTY;
        }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.LUCK_CONVERTER_CAPACITY.get(),
            CommonConfig.LUCK_CONVERTER_MAX_RECEIVE.get(), this::setChanged);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> automation);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                case 4 -> luckLevel();
                case 5 -> status;
                case 6 -> isAdvanced() ? 1 : 0;
                case 7 -> operationsPerInput();
                case 8 -> energyPerTick();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 5) status = value;
        }

        @Override public int getCount() { return 9; }
    };

    public LuckConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LUCK_CONVERTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  LuckConverterBlockEntity entity) {
        entity.tickServer(level);
    }

    private void tickServer(Level level) {
        chargeFromItem();
        if (level.getGameTime() % 10L == 0L && hasUpgrade(LuckConverterUpgradeItem.Type.AUTO_EJECT)) {
            autoEject(level);
        }

        if (luckLevel() <= 0) {
            reset(STATUS_NO_MODULE);
            return;
        }
        if (!hasInput()) {
            reset(STATUS_NO_VALID_INPUT);
            return;
        }

        maxProgress = operationLength();
        int cost = energyPerTick();
        if (energy.getEnergyStored() < cost) {
            status = STATUS_NO_ENERGY;
            setChanged();
            return;
        }

        status = STATUS_RUNNING;
        energy.extractEnergy(cost, false);
        progress++;
        setChanged();
        if (progress < maxProgress) return;

        List<PendingInput> work = collectWork();
        if (work.isEmpty()) {
            reset(STATUS_NO_VALID_INPUT);
            return;
        }
        List<ItemStack> results = createResults(work);
        if (!canInsertAll(results)) {
            progress = maxProgress;
            status = STATUS_OUTPUT_FULL;
            setChanged();
            return;
        }

        for (PendingInput input : work) items.extractItem(input.slot(), input.amount(), false);
        for (ItemStack stack : results) insertOutput(stack, false);
        progress = 0;
        status = STATUS_IDLE;
        setChanged();
    }

    private List<PendingInput> collectWork() {
        List<PendingInput> work = new ArrayList<>();
        int amount = operationsPerInput();
        for (int slot = 0; slot < inputSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!(stack.getItem() instanceof BlockItem)) continue;
            work.add(new PendingInput(slot, Math.min(amount, stack.getCount()), stack.copy()));
        }
        return work;
    }

    private List<ItemStack> createResults(List<PendingInput> work) {
        List<ItemStack> result = new ArrayList<>();
        if (!(level instanceof ServerLevel serverLevel)) return result;
        ItemStack tool = fortuneTool();
        boolean doubleDrops = hasUpgrade(LuckConverterUpgradeItem.Type.DOUBLE);
        boolean smeltDrops = hasUpgrade(LuckConverterUpgradeItem.Type.SMELT);

        for (PendingInput input : work) {
            BlockItem blockItem = (BlockItem) input.stack().getItem();
            BlockState state = blockItem.getBlock().defaultBlockState();
            for (int operation = 0; operation < input.amount(); operation++) {
                List<ItemStack> drops = Block.getDrops(
                        state, serverLevel, worldPosition, null, null, tool);
                if (drops.isEmpty()) drops = List.of(new ItemStack(blockItem));
                for (ItemStack drop : drops) {
                    ItemStack converted = smeltDrops ? smelt(drop) : drop.copy();
                    if (doubleDrops) converted.setCount(converted.getCount() * 2);
                    merge(result, converted);
                }
            }
        }
        return result;
    }

    private ItemStack smelt(ItemStack input) {
        if (level == null || input.isEmpty()) return input.copy();
        SimpleContainer container = new SimpleContainer(input.copy());
        Optional<SmeltingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, container, level);
        if (recipe.isEmpty()) return input.copy();
        ItemStack output = recipe.get().getResultItem(level.registryAccess()).copy();
        if (output.isEmpty()) return input.copy();
        output.setCount(Math.max(1, output.getCount() * input.getCount()));
        return output;
    }

    private static void merge(List<ItemStack> list, ItemStack addition) {
        if (addition.isEmpty()) return;
        ItemStack remaining = addition.copy();
        for (ItemStack existing : list) {
            if (!ItemStack.isSameItemSameTags(existing, remaining)) continue;
            int move = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
            existing.grow(move);
            remaining.shrink(move);
            if (remaining.isEmpty()) return;
        }
        while (!remaining.isEmpty()) {
            int count = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack split = remaining.copy();
            split.setCount(count);
            list.add(split);
            remaining.shrink(count);
        }
    }

    private ItemStack fortuneTool() {
        ItemStack module = items.getStackInSlot(MODULE_SLOT);
        if (module.getItem() instanceof PickaxeItem) return module.copy();
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        int fortune = luckLevel();
        if (fortune > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        return tool;
    }

    private boolean hasInput() {
        for (int slot = 0; slot < inputSlots(); slot++) {
            if (items.getStackInSlot(slot).getItem() instanceof BlockItem) return true;
        }
        return false;
    }

    private int luckLevel() {
        ItemStack stack = items.getStackInSlot(MODULE_SLOT);
        if (stack.getItem() instanceof LuckModuleItem module) return module.fortuneLevel();
        if (stack.getItem() instanceof PickaxeItem) {
            return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, stack);
        }
        return 0;
    }

    private int operationLength() {
        int base = isAdvanced() ? CommonConfig.ADVANCED_LUCK_CONVERTER_TIME.get()
                : CommonConfig.LUCK_CONVERTER_TIME.get();
        int speed = upgradeCount(LuckConverterUpgradeItem.Type.SPEED);
        return Math.max(5, base * 100 / (100 + speed * 25));
    }

    private int operationsPerInput() {
        return (isAdvanced() ? 2 : 1) + upgradeCount(LuckConverterUpgradeItem.Type.OPERATIONS);
    }

    private int energyPerTick() {
        int efficiency = upgradeCount(LuckConverterUpgradeItem.Type.EFFICIENCY);
        int percent = Math.max(20, 100 - efficiency * 10);
        return Math.max(1, CommonConfig.LUCK_CONVERTER_ENERGY_PER_TICK.get()
                * operationsPerInput() * percent / 100);
    }

    private int upgradeCount(LuckConverterUpgradeItem.Type type) {
        int count = 0;
        for (int slot = FIRST_UPGRADE; slot < ENERGY_SLOT; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.getItem() instanceof LuckConverterUpgradeItem upgrade && upgrade.type() == type) {
                count += stack.getCount();
            }
        }
        return Math.min(type.maximum(), count);
    }

    private boolean hasUpgrade(LuckConverterUpgradeItem.Type type) {
        return upgradeCount(type) > 0;
    }

    private void chargeFromItem() {
        ItemStack stack = items.getStackInSlot(ENERGY_SLOT);
        if (stack.isEmpty() || energy.getEnergyStored() >= energy.getMaxEnergyStored()) return;
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            int request = Math.min(CommonConfig.LUCK_CONVERTER_MAX_RECEIVE.get(),
                    energy.getMaxEnergyStored() - energy.getEnergyStored());
            int available = source.extractEnergy(request, true);
            int accepted = energy.receiveEnergy(available, false);
            if (accepted > 0) source.extractEnergy(accepted, false);
        });
    }

    private boolean canInsertAll(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(outputSlots());
        for (int index = 0; index < outputSlots(); index++) {
            copy.setStackInSlot(index, items.getStackInSlot(FIRST_OUTPUT + index).copy());
        }
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            for (int index = 0; index < outputSlots() && !remaining.isEmpty(); index++) {
                remaining = copy.insertItem(index, remaining, false);
            }
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }

    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = FIRST_OUTPUT;
             slot < FIRST_OUTPUT + outputSlots() && !remaining.isEmpty(); slot++) {
            remaining = items.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private void autoEject(Level level) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;
            Optional<IItemHandler> target = neighbor.getCapability(
                    ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;
            for (int slot = FIRST_OUTPUT; slot < FIRST_OUTPUT + outputSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                        target.get(), stack.copy(), false);
                if (remainder.getCount() != stack.getCount()) {
                    items.setStackInSlot(slot, remainder);
                }
            }
        }
    }

    private void reset(int newStatus) {
        if (progress != 0 || maxProgress != 0 || status != newStatus) {
            progress = 0;
            maxProgress = 0;
            status = newStatus;
            setChanged();
        }
    }

    public boolean isAdvanced() {
        return getBlockState().is(ModBlocks.ADVANCED_LUCK_CONVERTER.get());
    }

    public int inputSlots() { return isAdvanced() ? 72 : 30; }
    public int outputSlots() { return isAdvanced() ? 60 : 30; }
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
        return Component.translatable(isAdvanced()
                ? "container.metatech_reborn.advanced_luck_converter"
                : "container.metatech_reborn.luck_converter");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                       @NotNull Player player) {
        return new LuckConverterMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Status", status);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        status = tag.getInt("Status");
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

    private record PendingInput(int slot, int amount, ItemStack stack) {}
}
