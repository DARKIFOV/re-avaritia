package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.item.PatternCapacityUpgradeItem;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class MolecularAssemblerMenu extends AbstractContainerMenu {
    public static final int UNLOCK_BUTTON_ID = 0;
    public static final int PATTERN_MENU_START = MolecularAssemblerBlockEntity.TOTAL_SLOTS;
    public static final int PATTERN_UPGRADE_MENU_SLOT =
            PATTERN_MENU_START + MolecularAssemblerBlockEntity.MAX_PATTERN_SLOTS;
    public static final int SPEED_CARD_MENU_START = PATTERN_UPGRADE_MENU_SLOT + 1;
    public static final int MACHINE_MENU_SLOTS =
            SPEED_CARD_MENU_START + MolecularAssemblerBlockEntity.AE2_SPEED_CARD_SLOTS;

    private final MolecularAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public MolecularAssemblerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (MolecularAssemblerBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(9));
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
                        10 + column * 18, 26 + row * 18));
            }
        }

        addSlot(new SlotItemHandler(blockEntity.getItems(), MolecularAssemblerBlockEntity.OUTPUT_SLOT,
                194, 72) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(blockEntity.getItems(), MolecularAssemblerBlockEntity.ENERGY_SLOT,
                226, 72));

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int patternSlot = column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getPatternItems(), patternSlot,
                        304 + column * 18, 28 + row * 18));
            }
        }
        addSlot(new SlotItemHandler(blockEntity.getPatternUpgradeItems(), 0, 304, 110) {
            @Override public boolean mayPickup(@NotNull Player player) { return canRemovePatternUpgrade(); }
        });
        for (int slot = 0; slot < MolecularAssemblerBlockEntity.AE2_SPEED_CARD_SLOTS; slot++) {
            addSlot(new SlotItemHandler(blockEntity.getAe2SpeedCards(), slot, 334 + slot * 20, 110));
        }

        int inventoryY = 202;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        10 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    10 + column * 18, inventoryY + 58));
        }

        addDataSlots(data);
    }

    private boolean canRemovePatternUpgrade() {
        for (int slot = MolecularAssemblerBlockEntity.BASE_PATTERN_SLOTS;
             slot < MolecularAssemblerBlockEntity.MAX_PATTERN_SLOTS; slot++) {
            if (!blockEntity.getPatternItems().getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
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
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index < MACHINE_MENU_SLOTS) {
            if (!moveItemStackTo(original, MACHINE_MENU_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (original.getItem() instanceof PatternCapacityUpgradeItem) {
            if (!moveItemStackTo(original, PATTERN_UPGRADE_MENU_SLOT,
                    PATTERN_UPGRADE_MENU_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (MolecularAssemblerBlockEntity.isAe2SpeedCard(original)) {
            if (!moveItemStackTo(original, SPEED_CARD_MENU_START,
                    MACHINE_MENU_SLOTS, false)) return ItemStack.EMPTY;
        } else if (original.getItem() instanceof EncodedExtremePatternItem) {
            if (!moveItemStackTo(original, PATTERN_MENU_START,
                    PATTERN_UPGRADE_MENU_SLOT, false)) return ItemStack.EMPTY;
        } else if (original.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            if (!moveItemStackTo(original, MolecularAssemblerBlockEntity.ENERGY_SLOT,
                    MolecularAssemblerBlockEntity.ENERGY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, MolecularAssemblerBlockEntity.GRID_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }

    public int getProgressPixels(int width) {
        int maximum = getMaxProgress();
        return maximum <= 0 ? 0 : Math.min(width, getProgress() * width / maximum);
    }

    public int getEnergyPixels(int height) {
        int capacity = data.get(3);
        return capacity <= 0 ? 0 : Math.min(height, data.get(2) * height / capacity);
    }

    public int getEnergyStored() { return data.get(2); }
    public int getEnergyCapacity() { return data.get(3); }
    public boolean isRecipeLocked() { return data.get(4) != 0; }
    public int getActivePatternSlots() { return data.get(5); }
    public int getInstalledPatternCount() { return data.get(6); }
    public int getStatus() { return data.get(7); }
    public int getAe2SpeedCards() { return data.get(8); }
}
