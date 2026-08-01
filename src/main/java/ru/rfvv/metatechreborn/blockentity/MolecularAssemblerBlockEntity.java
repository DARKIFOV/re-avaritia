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
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import ru.rfvv.metatechreborn.util.TrackingEnergyStorage;

import java.util.Optional;

public final class MolecularAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GRID_SIZE = 9;
    public static final int GRID_SLOTS = 81;
    public static final int OUTPUT_SLOT = 81;
    public static final int TOTAL_SLOTS = 82;

    private int progress;
    private int maxProgress;
    private ResourceLocation lockedRecipeId;
    private MachineRecipeMatch.Source lockedRecipeSource;
    private final NonNullList<ItemStack> lockedTemplate = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) { if (slot < GRID_SLOTS) progress = 0; setChanged(); }
        @Override public int getSlotLimit(int slot) { return slot < GRID_SLOTS ? 1 : 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            if (lockedRecipeId == null) return true;
            ItemStack template = lockedTemplate.get(slot);
            return !template.isEmpty() && ItemStack.isSameItemSameTags(template, stack);
        }
    };

    private final IItemHandler grid = new IItemHandler() {
        @Override public int getSlots() { return GRID_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return items.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return items.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
    };

    private final IItemHandler externalItems = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == OUTPUT_SLOT ? stack : items.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return items.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
    };

    private final TrackingEnergyStorage energy = new TrackingEnergyStorage(
            CommonConfig.ASSEMBLER_CAPACITY.get(), CommonConfig.ASSEMBLER_MAX_RECEIVE.get(), this::setChanged);
    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> externalItems);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energy);
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> maxProgress; case 2 -> energy.getEnergyStored();
            case 3 -> energy.getMaxEnergyStored(); case 4 -> lockedRecipeId == null ? 0 : 1; default -> 0; }; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; if (index == 1) maxProgress = value; }
        @Override public int getCount() { return 5; }
    };

    public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOLECULAR_ASSEMBLER_9X9.get(), pos, state);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, MolecularAssemblerBlockEntity entity) {
        entity.tickServer(level);
    }

    private void tickServer(Level level) {
        if (CommonConfig.AUTO_EJECT_OUTPUT.get() && level.getGameTime() % 5L == 0L) autoEject(level);
        Optional<MachineRecipeMatch> match = lockedRecipeId == null ? findAnyMatch(level) : findLockedMatch(level);
        if (match.isEmpty() || match.get().result().isEmpty()) { progress = 0; maxProgress = 0; return; }
        MachineRecipeMatch recipe = match.get();
        if (lockedRecipeId == null) lockRecipe(recipe);
        maxProgress = Math.max(1, recipe.craftTime());
        if (!canOutput(recipe.result()) || energy.getEnergyStored() < recipe.energyPerTick()) return;
        if (recipe.energyPerTick() > 0) energy.extractEnergy(recipe.energyPerTick(), false);
        progress++;
        setChanged();
        if (progress >= maxProgress) {
            Optional<MachineRecipeMatch> validated = findLockedMatch(level);
            if (validated.isPresent() && canOutput(validated.get().result())) completeCraft(validated.get());
            else progress = 0;
        }
    }

    private Optional<MachineRecipeMatch> findAnyMatch(Level level) {
        if (CommonConfig.ENABLE_AVARITIA_INTEGRATION.get() && ModList.get().isLoaded("avaritia")) {
            try {
                Optional<MachineRecipeMatch> result = AvaritiaIntegration.findMatch(level, grid);
                if (result.isPresent()) return result;
            } catch (LinkageError | RuntimeException error) {
                MetaTechReborn.LOGGER.error("Re-Avaritia integration failed", error);
            }
        }
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.MOLECULAR_ASSEMBLING_TYPE.get()).stream()
                .filter(recipe -> recipe.matches(grid)).findFirst().map(this::metaMatch);
    }

    private Optional<MachineRecipeMatch> findLockedMatch(Level level) {
        if (lockedRecipeId == null || lockedRecipeSource == null) return Optional.empty();
        if (lockedRecipeSource == MachineRecipeMatch.Source.AVARITIA) {
            if (!ModList.get().isLoaded("avaritia")) return Optional.empty();
            try { return AvaritiaIntegration.findMatchById(level, lockedRecipeId, grid); }
            catch (LinkageError | RuntimeException error) { return Optional.empty(); }
        }
        return level.getRecipeManager().byKey(lockedRecipeId)
                .filter(MolecularAssemblerRecipe.class::isInstance).map(MolecularAssemblerRecipe.class::cast)
                .filter(recipe -> recipe.matches(grid)).map(this::metaMatch);
    }

    private MachineRecipeMatch metaMatch(MolecularAssemblerRecipe recipe) {
        return new MachineRecipeMatch(recipe.getId(), MachineRecipeMatch.Source.METATECH, recipe.result(),
                recipe.getRemainingItems(grid), recipe.time(), recipe.energyPerTick());
    }

    private void lockRecipe(MachineRecipeMatch recipe) {
        lockedRecipeId = recipe.id(); lockedRecipeSource = recipe.source();
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot).copy();
            if (!stack.isEmpty()) stack.setCount(1);
            lockedTemplate.set(slot, stack);
        }
        setChanged();
    }
    public void clearRecipeLock() {
        lockedRecipeId = null; lockedRecipeSource = null; progress = 0; maxProgress = 0;
        for (int slot = 0; slot < GRID_SLOTS; slot++) lockedTemplate.set(slot, ItemStack.EMPTY);
        setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }
    private void completeCraft(MachineRecipeMatch recipe) {
        for (int slot = 0; slot < GRID_SLOTS; slot++) if (!items.getStackInSlot(slot).isEmpty()) items.extractItem(slot, 1, false);
        for (int slot = 0; slot < Math.min(GRID_SLOTS, recipe.remainingItems().size()); slot++) {
            ItemStack remainder = recipe.remainingItems().get(slot).copy();
            if (!remainder.isEmpty()) items.setStackInSlot(slot, remainder);
        }
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) items.setStackInSlot(OUTPUT_SLOT, recipe.result().copy());
        else { output.grow(recipe.result().getCount()); items.setStackInSlot(OUTPUT_SLOT, output); }
        progress = 0; setChanged();
    }

    private void autoEject(Level level) {
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            Optional<IItemHandler> target = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target.get(), output.copy(), false);
            if (remainder.getCount() != output.getCount()) items.setStackInSlot(OUTPUT_SLOT, remainder);
            if (remainder.isEmpty()) return;
            output = remainder;
        }
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) if (!items.getStackInSlot(slot).isEmpty()) drops.add(items.getStackInSlot(slot).copy());
        return drops;
    }
    @Override public @NotNull Component getDisplayName() { return Component.translatable("container.metatech_reborn.molecular_assembler_9x9"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MolecularAssemblerMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Inventory", items.serializeNBT()); tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress); tag.putInt("MaxProgress", maxProgress);
        if (lockedRecipeId != null && lockedRecipeSource != null) {
            tag.putString("LockedRecipe", lockedRecipeId.toString()); tag.putString("LockedSource", lockedRecipeSource.name());
        }
        ListTag list = new ListTag();
        for (int slot = 0; slot < GRID_SLOTS; slot++) if (!lockedTemplate.get(slot).isEmpty()) {
            CompoundTag entry = new CompoundTag(); entry.putByte("Slot", (byte) slot); lockedTemplate.get(slot).save(entry); list.add(entry);
        }
        tag.put("LockedTemplate", list);
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag); items.deserializeNBT(tag.getCompound("Inventory")); energy.setEnergyStored(tag.getInt("Energy"));
        progress = tag.getInt("Progress"); maxProgress = tag.getInt("MaxProgress");
        lockedRecipeId = ResourceLocation.tryParse(tag.getString("LockedRecipe"));
        try { lockedRecipeSource = MachineRecipeMatch.Source.valueOf(tag.getString("LockedSource")); }
        catch (IllegalArgumentException error) { lockedRecipeSource = null; }
        for (int slot = 0; slot < GRID_SLOTS; slot++) lockedTemplate.set(slot, ItemStack.EMPTY);
        ListTag list = tag.getList("LockedTemplate", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) { CompoundTag entry = list.getCompound(i); int slot = entry.getByte("Slot") & 255;
            if (slot < GRID_SLOTS) lockedTemplate.set(slot, ItemStack.of(entry)); }
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); energyCapability.invalidate(); }
}
