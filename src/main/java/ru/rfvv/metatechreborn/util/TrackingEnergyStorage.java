package ru.rfvv.metatechreborn.util;

import net.minecraftforge.energy.EnergyStorage;

/** Energy storage that marks its owning block entity dirty on every real change. */
public final class TrackingEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public TrackingEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
        // maxExtract is kept at zero for external capability semantics. The overridden
        // extraction method below is used by the owning machines for their real FE cost.
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
    public int extractEnergy(int requested, boolean simulate) {
        int extracted = Math.min(Math.max(0, requested), energy);
        if (!simulate && extracted != 0) {
            energy -= extracted;
            onChanged.run();
        }
        return extracted;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    public void setEnergyStored(int value) {
        this.energy = Math.max(0, Math.min(capacity, value));
        onChanged.run();
    }
}
