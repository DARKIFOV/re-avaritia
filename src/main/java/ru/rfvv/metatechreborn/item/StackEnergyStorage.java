package ru.rfvv.metatechreborn.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** ItemStack-backed Forge Energy storage used by the restored electric tools. */
public final class StackEnergyStorage implements IEnergyStorage, ICapabilityProvider {
    private static final String ENERGY_TAG = "MetaTechEnergy";

    private final ItemStack stack;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;
    private final LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> this);

    public StackEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this.stack = stack;
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(this.capacity - getEnergyStored(), Math.min(this.maxReceive, Math.max(0, maxReceive)));
        if (!simulate && received > 0) setEnergy(getEnergyStored() + received);
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.min(getEnergyStored(), Math.min(this.maxExtract, Math.max(0, maxExtract)));
        if (!simulate && extracted > 0) setEnergy(getEnergyStored() - extracted);
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return Math.min(capacity, Math.max(0, stack.getOrCreateTag().getInt(ENERGY_TAG)));
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    private void setEnergy(int energy) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(ENERGY_TAG, Math.min(capacity, Math.max(0, energy)));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY ? capability.cast() : LazyOptional.empty();
    }

    public static int getStored(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::getEnergyStored)
                .orElseGet(() -> Math.max(0, stack.getOrCreateTag().getInt(ENERGY_TAG)));
    }

    public static int getCapacity(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::getMaxEnergyStored)
                .orElse(0);
    }

    public static boolean consume(ItemStack stack, int amount) {
        if (amount <= 0) return true;
        return stack.getCapability(ForgeCapabilities.ENERGY).map(storage -> {
            if (storage.extractEnergy(amount, true) < amount) return false;
            storage.extractEnergy(amount, false);
            return true;
        }).orElse(false);
    }
}
