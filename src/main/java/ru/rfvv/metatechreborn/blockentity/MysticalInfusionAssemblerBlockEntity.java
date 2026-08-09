package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.integration.mystical.MysticalInfusionSupport;
import ru.rfvv.metatechreborn.item.EncodedMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.item.PatternCapacityUpgradeItem;
import ru.rfvv.metatechreborn.menu.MysticalInfusionAssemblerMenu;
import ru.rfvv.metatechreborn.pattern.MysticalInfusionPatternData;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MysticalInfusionAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int ENERGY_SLOT = 10;
    public static final int TOTAL_SLOTS = 11;
    public static final int BASE_PATTERN_SLOTS = 9;
    public static final int MAX_PATTERN_SLOTS = 36;
    public static final int SPEED_CARD_SLOTS = 4;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_RUNNING = 4;
    public static final int STATUS_AE2_READY = 5;

    private boolean suppressCallbacks;
    private int progress;
    private int maxProgress;
    private int status;
    private ResourceLocation lockedRecipeId;
    private ItemStack lockedResult = ItemStack.EMPTY;
    private final NonNullList<ItemStack> lockedTemplate = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            if (!suppressCallbacks && slot < INPUT_SLOTS) progress = 0;
            setChanged();
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            if (slot == ENERGY_SLOT) return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            return slot < INPUT_SLOTS && canInsertInput(slot, stack);
        }
        @Override public int getSlotLimit(int slot) { return slot == ENERGY_SLOT ? 1 : 64; }
    };

    private final ItemStackHandler patterns = new ItemStackHandler(MAX_PATTERN_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < getActivePatternSlots() && EncodedMysticalInfusionPatternItem.read(stack).isPresent();
        }
    };

    private final ItemStackHandler patternUpgrade = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof PatternCapacityUpgradeItem;
        }
    };

    private final ItemStackHandler speedCards = new ItemStackHandler(SPEED_CARD_SLOTS) {
        @Override protected void onContentsChanged(int slot) { progress = 0; maxProgress = 0; setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isSpeedCard(stack); }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.ASSEMBLER_CAPACITY.get(), CommonConfig.ASSEMBLER_MAX_RECEIVE.get(), this::setChanged);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                case 4 -> getActivePatternSlots();
                case 5 -> getInstalledPatternCount();
                case 6 -> status;
                case 7 -> getSpeedCardCount();
                case 8 -> lockedRecipeId == null ? 0 : 1;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            if (i == 0) progress = value; else if (i == 1) maxProgress = value; else if (i == 6) status = value;
        }
        @Override public int getCount() { return 9; }
    };

    public MysticalInfusionAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMysticalInfusion.ASSEMBLER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MysticalInfusionAssemblerBlockEntity be) {
        be.tickServer(level);
    }

    private void tickServer(Level level) {
        chargeFromEnergyItem();
        if (CommonConfig.AUTO_EJECT_OUTPUT.get() && level.getGameTime() % 5L == 0L) autoEject(level);

        Optional<MysticalInfusionSupport.View> match = currentRecipe(level);
        if (match.isEmpty()) {
            progress = maxProgress = 0;
            if (!isInputEmpty()) lockedRecipeId = null;
            setStatus(isInputEmpty() && getInstalledPatternCount() > 0 ? STATUS_AE2_READY
                    : isInputEmpty() ? STATUS_IDLE : STATUS_NO_RECIPE);
            return;
        }
        var view = match.get();
        if (lockedRecipeId == null) lock(view);
        ItemStack result = MysticalInfusionSupport.assemble(level, view, inputCopies());
        if (result.isEmpty()) result = view.output().copy();
        if (!canOutput(result)) { setStatus(STATUS_OUTPUT_FULL); return; }

        int multiplier = speedMultiplier();
        maxProgress = Math.max(1, (CommonConfig.DEFAULT_CRAFT_TIME.get() + multiplier - 1) / multiplier);
        int fe = (int) Math.min(Integer.MAX_VALUE,
                (long) CommonConfig.DEFAULT_ENERGY_PER_TICK.get() * multiplier);
        if (energy.getEnergyStored() < fe) { setStatus(STATUS_NO_ENERGY); return; }

        setStatus(STATUS_RUNNING);
        if (fe > 0) energy.extractEnergy(fe, false);
        progress++;
        setChanged();
        if (progress >= maxProgress) complete(level, view, result);
    }

    private Optional<MysticalInfusionSupport.View> currentRecipe(Level level) {
        List<ItemStack> inputs = inputCopies();
        if (lockedRecipeId != null) {
            Optional<MysticalInfusionSupport.View> locked = MysticalInfusionSupport.find(level, lockedRecipeId);
            if (locked.isPresent() && MysticalInfusionSupport.matches(level, locked.get(), inputs)) return locked;
            lockedRecipeId = null;
        }
        return MysticalInfusionSupport.findMatching(level, inputs);
    }

    private void lock(MysticalInfusionSupport.View view) {
        lockedRecipeId = view.id();
        lockedResult = view.output().copy();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty()) lockedTemplate.set(i, ItemStack.EMPTY);
            else { ItemStack copy = stack.copy(); copy.setCount(1); lockedTemplate.set(i, copy); }
        }
        setChanged();
    }

    public void clearRecipeLock() {
        lockedRecipeId = null;
        lockedResult = ItemStack.EMPTY;
        for (int i = 0; i < INPUT_SLOTS; i++) lockedTemplate.set(i, ItemStack.EMPTY);
        progress = maxProgress = 0;
        setStatus(STATUS_IDLE);
    }

    private void complete(Level level, MysticalInfusionSupport.View view, ItemStack result) {
        NonNullList<ItemStack> remaining = MysticalInfusionSupport.remaining(level, view, inputCopies());
        suppressCallbacks = true;
        try {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                ItemStack stack = items.getStackInSlot(i);
                if (!stack.isEmpty()) stack.shrink(1);
                if (stack.isEmpty() && i < remaining.size() && !remaining.get(i).isEmpty()) {
                    items.setStackInSlot(i, remaining.get(i).copy());
                }
            }
            insertOutput(result.copy());
        } finally { suppressCallbacks = false; }
        progress = 0;
        setStatus(getInstalledPatternCount() > 0 ? STATUS_AE2_READY : STATUS_IDLE);
        setChanged();
    }

    private List<ItemStack> inputCopies() {
        List<ItemStack> result = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) result.add(items.getStackInSlot(i).copy());
        return result;
    }

    public int getActivePatternSlots() {
        return patternUpgrade.getStackInSlot(0).getItem() instanceof PatternCapacityUpgradeItem
                ? MAX_PATTERN_SLOTS : BASE_PATTERN_SLOTS;
    }
    public int getInstalledPatternCount() {
        int count = 0;
        for (int i = 0; i < getActivePatternSlots(); i++)
            if (EncodedMysticalInfusionPatternItem.read(patterns.getStackInSlot(i)).isPresent()) count++;
        return count;
    }
    public int getSpeedCardCount() {
        int count = 0;
        for (int i = 0; i < SPEED_CARD_SLOTS; i++) if (isSpeedCard(speedCards.getStackInSlot(i))) count++;
        return count;
    }
    private int speedMultiplier() {
        for (int i = 0; i < SPEED_CARD_SLOTS; i++) if (isSuperSpeedCard(speedCards.getStackInSlot(i))) return 512;
        return 1 + getSpeedCardCount();
    }
    public static boolean isSpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ("ae2".equals(id.getNamespace()) && "speed_card".equals(id.getPath())) || isSuperSpeedCard(stack);
    }
    private static boolean isSuperSpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "ae2_overclocked".equals(id.getNamespace()) && "super_speed_card".equals(id.getPath());
    }

    public boolean canAcceptAe2Plan() { return progress == 0 && isInputEmpty() && canOutput(ItemStack.EMPTY); }

    public boolean acceptExternalPatternBatch(MysticalInfusionPatternData pattern, List<ItemStack> supplied,
                                              ItemStack requestedOutput, long requestedAmount) {
        if (!canAcceptAe2Plan() || requestedOutput.isEmpty()
                || !ItemStack.isSameItemSameTags(pattern.output(), requestedOutput)
                || requestedAmount < pattern.output().getCount()) return false;
        Optional<NonNullList<ItemStack>> placement = createPlacement(pattern.inputs(), supplied);
        if (placement.isEmpty()) return false;
        suppressCallbacks = true;
        try {
            for (int i = 0; i < INPUT_SLOTS; i++) items.setStackInSlot(i, placement.get().get(i));
        } finally { suppressCallbacks = false; }
        lockedRecipeId = pattern.recipeId();
        lockedResult = pattern.output().copy();
        for (int i = 0; i < INPUT_SLOTS; i++) lockedTemplate.set(i, pattern.inputs().get(i).copy());
        status = STATUS_RUNNING;
        setChanged();
        return true;
    }

    private Optional<NonNullList<ItemStack>> createPlacement(List<ItemStack> template, List<ItemStack> source) {
        List<ItemStack> supplied = new ArrayList<>();
        for (ItemStack stack : source) if (!stack.isEmpty()) supplied.add(stack.copy());
        NonNullList<ItemStack> result = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack expected = template.get(slot);
            if (expected.isEmpty()) continue;
            int found = -1;
            for (int i = 0; i < supplied.size(); i++) {
                if (!supplied.get(i).isEmpty() && ItemStack.isSameItemSameTags(expected, supplied.get(i))) { found = i; break; }
            }
            if (found < 0) return Optional.empty();
            ItemStack placed = supplied.get(found).copy(); placed.setCount(1); result.set(slot, placed);
            supplied.get(found).shrink(1);
        }
        for (ItemStack stack : supplied) if (!stack.isEmpty()) return Optional.empty();
        return Optional.of(result);
    }

    private boolean canInsertInput(int slot, ItemStack stack) {
        if (lockedRecipeId == null) return true;
        ItemStack expected = lockedTemplate.get(slot);
        return !expected.isEmpty() && ItemStack.isSameItemSameTags(expected, stack);
    }
    private boolean isInputEmpty() {
        for (int i = 0; i < INPUT_SLOTS; i++) if (!items.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    private boolean canOutput(ItemStack result) {
        ItemStack current = items.getStackInSlot(OUTPUT_SLOT);
        if (result.isEmpty()) return current.isEmpty() || current.getCount() < current.getMaxStackSize();
        if (current.isEmpty()) return result.getCount() <= result.getMaxStackSize();
        return ItemStack.isSameItemSameTags(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }
    private void insertOutput(ItemStack result) {
        ItemStack current = items.getStackInSlot(OUTPUT_SLOT);
        if (current.isEmpty()) items.setStackInSlot(OUTPUT_SLOT, result);
        else { current.grow(result.getCount()); items.setStackInSlot(OUTPUT_SLOT, current); }
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

    private void autoEject(Level level) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            Optional<IItemHandler> target = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER,
                    direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(target.get(), output.copy(), false);
            if (rest.getCount() != output.getCount()) { items.setStackInSlot(OUTPUT_SLOT, rest); setChanged(); return; }
        }
    }

    private void setStatus(int next) { if (status != next) { status = next; setChanged(); } }

    public ItemStackHandler getItems() { return items; }
    public ItemStackHandler getPatternItems() { return patterns; }
    public ItemStackHandler getPatternUpgradeItems() { return patternUpgrade; }
    public ItemStackHandler getSpeedCards() { return speedCards; }
    public ContainerData getData() { return data; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> result = NonNullList.create();
        for (int i = 0; i < TOTAL_SLOTS; i++) if (!items.getStackInSlot(i).isEmpty()) result.add(items.getStackInSlot(i).copy());
        for (int i = 0; i < MAX_PATTERN_SLOTS; i++) if (!patterns.getStackInSlot(i).isEmpty()) result.add(patterns.getStackInSlot(i).copy());
        if (!patternUpgrade.getStackInSlot(0).isEmpty()) result.add(patternUpgrade.getStackInSlot(0).copy());
        for (int i = 0; i < SPEED_CARD_SLOTS; i++) if (!speedCards.getStackInSlot(i).isEmpty()) result.add(speedCards.getStackInSlot(i).copy());
        return result;
    }

    @Override public @NotNull Component getDisplayName() { return Component.literal("Молекулярный сборщик мистического алтаря"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MysticalInfusionAssemblerMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Patterns", patterns.serializeNBT());
        tag.put("PatternUpgrade", patternUpgrade.serializeNBT());
        tag.put("SpeedCards", speedCards.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored()); tag.putInt("Progress", progress); tag.putInt("Status", status);
        if (lockedRecipeId != null) tag.putString("LockedRecipe", lockedRecipeId.toString());
        if (!lockedResult.isEmpty()) tag.put("LockedResult", lockedResult.save(new CompoundTag()));
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) items.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Patterns", Tag.TAG_COMPOUND)) patterns.deserializeNBT(tag.getCompound("Patterns"));
        if (tag.contains("PatternUpgrade", Tag.TAG_COMPOUND)) patternUpgrade.deserializeNBT(tag.getCompound("PatternUpgrade"));
        if (tag.contains("SpeedCards", Tag.TAG_COMPOUND)) speedCards.deserializeNBT(tag.getCompound("SpeedCards"));
        energy.setEnergyStored(tag.getInt("Energy")); progress = tag.getInt("Progress"); status = tag.getInt("Status");
        lockedRecipeId = tag.contains("LockedRecipe", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("LockedRecipe")) : null;
        lockedResult = tag.contains("LockedResult", Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("LockedResult")) : ItemStack.EMPTY;
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); energyCapability.invalidate(); }
}
