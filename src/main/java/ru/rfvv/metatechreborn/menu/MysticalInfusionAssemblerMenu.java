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
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionAssemblerBlockEntity;
import ru.rfvv.metatechreborn.item.EncodedMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.item.PatternCapacityUpgradeItem;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

public final class MysticalInfusionAssemblerMenu extends AbstractContainerMenu {
    public static final int PATTERN_START = MysticalInfusionAssemblerBlockEntity.TOTAL_SLOTS;
    public static final int UPGRADE_SLOT = PATTERN_START + MysticalInfusionAssemblerBlockEntity.MAX_PATTERN_SLOTS;
    public static final int SPEED_START = UPGRADE_SLOT + 1;
    public static final int PLAYER_START = SPEED_START + MysticalInfusionAssemblerBlockEntity.SPEED_CARD_SLOTS;

    private final MysticalInfusionAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public MysticalInfusionAssemblerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (MysticalInfusionAssemblerBlockEntity) inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(9));
    }

    public MysticalInfusionAssemblerMenu(int id, Inventory inventory,
                                         MysticalInfusionAssemblerBlockEntity blockEntity, ContainerData data) {
        super(ModMysticalInfusion.ASSEMBLER_MENU.get(), id);
        this.blockEntity = blockEntity; this.data = data;

        int[] map = {4,0,1,2,3,5,6,7,8};
        for (int slot = 0; slot < 9; slot++) {
            int grid = map[slot];
            addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                    20 + (grid % 3) * 36, 28 + (grid / 3) * 36));
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), MysticalInfusionAssemblerBlockEntity.OUTPUT_SLOT, 148, 64) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(blockEntity.getItems(), MysticalInfusionAssemblerBlockEntity.ENERGY_SLOT, 184, 64));

        for (int i = 0; i < 36; i++) addSlot(new SlotItemHandler(blockEntity.getPatternItems(), i,
                236 + (i % 9) * 18, 28 + (i / 9) * 18));
        addSlot(new SlotItemHandler(blockEntity.getPatternUpgradeItems(), 0, 236, 112) {
            @Override public boolean mayPickup(@NotNull Player player) { return canRemoveUpgrade(); }
        });
        for (int i = 0; i < 4; i++) addSlot(new SlotItemHandler(blockEntity.getSpeedCards(), i, 270 + i * 20, 112));

        int invX = 116, invY = 174;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
        addDataSlots(data);
    }

    private boolean canRemoveUpgrade() {
        for (int i = MysticalInfusionAssemblerBlockEntity.BASE_PATTERN_SLOTS;
             i < MysticalInfusionAssemblerBlockEntity.MAX_PATTERN_SLOTS; i++)
            if (!blockEntity.getPatternItems().getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModMysticalInfusion.ASSEMBLER.get());
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PatternCapacityUpgradeItem) {
            if (!moveItemStackTo(stack, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (MysticalInfusionAssemblerBlockEntity.isSpeedCard(stack)) {
            if (!moveItemStackTo(stack, SPEED_START, PLAYER_START, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof EncodedMysticalInfusionPatternItem) {
            if (!moveItemStackTo(stack, PATTERN_START, UPGRADE_SLOT, false)) return ItemStack.EMPTY;
        } else if (stack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            if (!moveItemStackTo(stack, MysticalInfusionAssemblerBlockEntity.ENERGY_SLOT,
                    MysticalInfusionAssemblerBlockEntity.ENERGY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, MysticalInfusionAssemblerBlockEntity.INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getEnergy() { return data.get(2); }
    public int getEnergyCapacity() { return data.get(3); }
    public int getActivePatternSlots() { return data.get(4); }
    public int getInstalledPatterns() { return data.get(5); }
    public int getStatus() { return data.get(6); }
    public int getSpeedCards() { return data.get(7); }
    public boolean isLocked() { return data.get(8) != 0; }
}
