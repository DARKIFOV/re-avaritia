package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class NeutroniumCombinerMenu extends AbstractContainerMenu {
    private final NeutroniumCombinerBlockEntity blockEntity;
    private final ContainerData data;

    public NeutroniumCombinerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (NeutroniumCombinerBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(32));
    }

    public NeutroniumCombinerMenu(int containerId, Inventory playerInventory,
                                  NeutroniumCombinerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.NEUTRONIUM_COMBINER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        12 + column * 30, 30 + row * 30));
            }
        }

        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 8; column++) {
                int slot = NeutroniumCombinerBlockEntity.FIRST_OUTPUT_SLOT + column + row * 8;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        126 + column * 18, 28 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        for (int column = 0; column < NeutroniumCombinerBlockEntity.UPGRADE_SLOTS; column++) {
            int slot = NeutroniumCombinerBlockEntity.FIRST_UPGRADE_SLOT + column;
            addSlot(new SlotItemHandler(blockEntity.getItems(), slot, 12 + column * 22, 130));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(),
                NeutroniumCombinerBlockEntity.ENERGY_SLOT, 104, 130));

        int inventoryX = 91;
        int inventoryY = 196;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        inventoryX + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    inventoryX + column * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.NEUTRONIUM_COMBINER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = NeutroniumCombinerBlockEntity.TOTAL_SLOTS;

        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getProgress(int inputSlot) {
        return data.get(inputSlot);
    }

    public int getMaxProgress(int inputSlot) {
        return data.get(NeutroniumCombinerBlockEntity.INPUT_SLOTS + inputSlot);
    }

    public int getProgressPixels(int inputSlot, int width) {
        int maximum = getMaxProgress(inputSlot);
        return maximum <= 0 ? 0 : Math.min(width, getProgress(inputSlot) * width / maximum);
    }

    public int getStatus(int inputSlot) {
        return data.get(NeutroniumCombinerBlockEntity.INPUT_SLOTS * 2 + inputSlot);
    }

    public int getEnergyStored() { return data.get(27); }
    public int getEnergyCapacity() { return data.get(28); }
    public int getSpeedUpgrades() { return data.get(29); }
    public int getEfficiencyUpgrades() { return data.get(30); }
    public int getOutputUpgrades() { return data.get(31); }

    public int getEnergyPixels(int height) {
        int capacity = getEnergyCapacity();
        return capacity <= 0 ? 0 : Math.min(height, getEnergyStored() * height / capacity);
    }
}
