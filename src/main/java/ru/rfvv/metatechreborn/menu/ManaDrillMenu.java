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
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class ManaDrillMenu extends AbstractContainerMenu {
    private final ManaDrillBlockEntity blockEntity;
    private final ContainerData data;

    public ManaDrillMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (ManaDrillBlockEntity) inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(9));
    }

    public ManaDrillMenu(int id, Inventory inventory, ManaDrillBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.MANA_DRILL.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getItems(), ManaDrillBlockEntity.MODULE_SLOT, 20, 34));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ManaDrillBlockEntity.SPEED_SLOT, 20, 64));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ManaDrillBlockEntity.LOOTING_SLOT, 44, 64));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ManaDrillBlockEntity.GENERATION_SLOT, 68, 64));

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = ManaDrillBlockEntity.FIRST_OUTPUT_SLOT + column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        132 + column * 18, 30 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        int playerY = 240;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        81 + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 81 + column * 18, playerY + 58));
        }
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MANA_DRILL.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = ManaDrillBlockEntity.TOTAL_SLOTS;
        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, ManaDrillBlockEntity.FIRST_OUTPUT_SLOT, false)) {
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
    public int getSpeedLevel() { return data.get(4); }
    public int getLootingLevel() { return data.get(5); }
    public int getGenerationLevel() { return data.get(6); }
    public boolean isStructureFormed() { return data.get(7) != 0; }
    public int getStatus() { return data.get(8); }

    public int getProgressPixels(int width) {
        return getMaxProgress() <= 0 ? 0 : Math.min(width, getProgress() * width / getMaxProgress());
    }

    public int getManaPixels(int height) {
        return getManaCapacity() <= 0 ? 0 : Math.min(height, getMana() * height / getManaCapacity());
    }
}
