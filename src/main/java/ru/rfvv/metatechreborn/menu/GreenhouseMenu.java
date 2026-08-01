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
import ru.rfvv.metatechreborn.blockentity.GreenhouseBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class GreenhouseMenu extends AbstractContainerMenu {
    private final GreenhouseBlockEntity blockEntity;
    private final ContainerData data;

    public GreenhouseMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (GreenhouseBlockEntity) inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(10));
    }

    public GreenhouseMenu(int id, Inventory inventory, GreenhouseBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.GREENHOUSE.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getItems(), GreenhouseBlockEntity.FLOWER_SLOT, 20, 27));
        for (int column = 0; column < GreenhouseBlockEntity.MODULE_SLOTS; column++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(),
                    GreenhouseBlockEntity.FIRST_MODULE_SLOT + column, 20 + column * 24, 57));
        }
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = GreenhouseBlockEntity.FIRST_FUEL_SLOT + column + row * 3;
                addSlot(new SlotItemHandler(blockEntity.getItems(), index,
                        118 + column * 18, 26 + row * 18));
            }
        }

        int playerY = 133;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        62 + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 62 + column * 18, playerY + 58));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.GREENHOUSE.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = GreenhouseBlockEntity.TOTAL_SLOTS;
        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getMana() { return data.get(2); }
    public int getManaCapacity() { return data.get(3); }
    public int getFluidAmount() { return data.get(4); }
    public int getFluidCapacity() { return data.get(5); }
    public int getSpeedLevel() { return data.get(6); }
    public int getEfficiencyLevel() { return data.get(7); }
    public int getEconomyLevel() { return data.get(8); }
    public int getModeId() { return data.get(9); }

    public int getProgressPixels(int width) {
        return getMaxProgress() <= 0 ? 0 : Math.min(width, getProgress() * width / getMaxProgress());
    }

    public int getManaPixels(int height) {
        return getManaCapacity() <= 0 ? 0 : Math.min(height, getMana() * height / getManaCapacity());
    }

    public int getFluidPixels(int height) {
        return getFluidCapacity() <= 0 ? 0 : Math.min(height, getFluidAmount() * height / getFluidCapacity());
    }
}
