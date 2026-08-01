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
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class MolecularAssemblerMenu extends AbstractContainerMenu {
    public static final int UNLOCK_BUTTON_ID = 0;

    private final MolecularAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public MolecularAssemblerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (MolecularAssemblerBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(5));
    }

    public MolecularAssemblerMenu(int containerId, Inventory playerInventory,
                                  MolecularAssemblerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.MOLECULAR_ASSEMBLER_9X9.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        8 + column * 18, 15 + row * 18));
            }
        }

        addSlot(new SlotItemHandler(blockEntity.getItems(), MolecularAssemblerBlockEntity.OUTPUT_SLOT,
                191, 74) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        int inventoryY = 179;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }

        addDataSlots(data);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.MOLECULAR_ASSEMBLER_9X9.get());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == UNLOCK_BUTTON_ID) {
            blockEntity.clearRecipeLock();
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = MolecularAssemblerBlockEntity.TOTAL_SLOTS;

        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(original, 0, MolecularAssemblerBlockEntity.GRID_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getProgressPixels(int width) {
        int maximum = data.get(1);
        return maximum <= 0 ? 0 : Math.min(width, data.get(0) * width / maximum);
    }

    public int getEnergyPixels(int height) {
        int capacity = data.get(3);
        return capacity <= 0 ? 0 : Math.min(height, data.get(2) * height / capacity);
    }

    public int getEnergyStored() {
        return data.get(2);
    }

    public int getEnergyCapacity() {
        return data.get(3);
    }

    public boolean isRecipeLocked() {
        return data.get(4) != 0;
    }
}
