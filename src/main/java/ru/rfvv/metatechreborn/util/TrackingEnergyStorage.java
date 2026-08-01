package ru.rfvv.metatechreborn.util;

import net.minecraftforge.energy.EnergyStorage;

public final class TrackingEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public TrackingEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
        super(capacity, maxReceive, 0);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (!simulate && received != 0) onChanged.run();
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate && extracted != 0) onChanged.run();
        return extracted;
    }

    public void setEnergyStored(int value) {
        this.energy = Math.max(0, Math.min(capacity, value));
        onChanged.run();
    }
}
