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
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class LuckConverterMenu extends AbstractContainerMenu {
    private final LuckConverterBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean advanced;
    private final int machineMenuSlots;

    public LuckConverterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (LuckConverterBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(9));
    }

    public LuckConverterMenu(int id, Inventory inventory, LuckConverterBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.LUCK_CONVERTER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;
        this.advanced = blockEntity.isAdvanced();

        int columns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        int outputRows = advanced ? 5 : 3;
        int inputY = 20;
        int outputY = advanced ? 134 : 84;
        for (int row = 0; row < inputRows; row++) {
            for (int column = 0; column < columns; column++) {
                int handlerSlot = column + row * columns;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot, 10 + column * 18, inputY + row * 18));
            }
        }
        for (int row = 0; row < outputRows; row++) {
            for (int column = 0; column < columns; column++) {
                int handlerSlot = LuckConverterBlockEntity.FIRST_OUTPUT + column + row * columns;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot, 10 + column * 18, outputY + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        int sideX = advanced ? 238 : 194;
        addSlot(new SlotItemHandler(blockEntity.getItems(), LuckConverterBlockEntity.MODULE_SLOT, sideX, 116));
        for (int i = 0; i < LuckConverterBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new SlotItemHandler(blockEntity.getItems(), LuckConverterBlockEntity.FIRST_UPGRADE + i,
                    sideX, 8 + i * 18));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), LuckConverterBlockEntity.ENERGY_SLOT,
                sideX, advanced ? 154 : 140));
        this.machineMenuSlots = slots.size();

        int playerX = advanced ? 37 : 28;
        int playerY = advanced ? 254 : 176;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, playerX + column * 18, playerY + 58));
        }
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player,
                advanced ? ModBlocks.ADVANCED_LUCK_CONVERTER.get() : ModBlocks.LUCK_CONVERTER.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < machineMenuSlots) {
            if (!moveItemStackTo(original, machineMenuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineMenuSlots, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public boolean isAdvanced() { return advanced; }
    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getEnergy() { return data.get(2); }
    public int getEnergyCapacity() { return data.get(3); }
    public int getLuckLevel() { return data.get(4); }
    public int getStatus() { return data.get(5); }
    public int getOperations() { return data.get(7); }
    public int getEnergyPerTick() { return data.get(8); }
    public int progressPixels(int width) { return getMaxProgress() <= 0 ? 0 : Math.min(width, getProgress() * width / getMaxProgress()); }
    public int energyPixels(int height) { return getEnergyCapacity() <= 0 ? 0 : Math.min(height, getEnergy() * height / getEnergyCapacity()); }
}
