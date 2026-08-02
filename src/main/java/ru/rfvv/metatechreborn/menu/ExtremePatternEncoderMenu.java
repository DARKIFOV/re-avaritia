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
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class ExtremePatternEncoderMenu extends AbstractContainerMenu {
    public static final int ENCODE_BUTTON_ID = 0;
    public static final int CLEAR_BUTTON_ID = 1;
    public static final int PLAYER_INVENTORY_START = ExtremePatternEncoderBlockEntity.TOTAL_SLOTS;

    private final ExtremePatternEncoderBlockEntity blockEntity;
    private final ContainerData data;

    public ExtremePatternEncoderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                (ExtremePatternEncoderBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(2));
    }

    public ExtremePatternEncoderMenu(int id, Inventory inventory,
                                     ExtremePatternEncoderBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.EXTREME_PATTERN_ENCODER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        10 + column * 18, 26 + row * 18));
            }
        }

        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremePatternEncoderBlockEntity.BLANK_SLOT,
                198, 64));
        addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremePatternEncoderBlockEntity.OUTPUT_SLOT,
                198, 106) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        int inventoryY = 202;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        10 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 10 + column * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.EXTREME_PATTERN_ENCODER.get());
    }

    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ENCODE_BUTTON_ID) return blockEntity.encode();
        if (id == CLEAR_BUTTON_ID) {
            blockEntity.clearGrid(player);
            return true;
        }
        return false;
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(original, PLAYER_INVENTORY_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (original.is(ModItems.BLANK_EXTREME_PATTERN.get())) {
            if (!moveItemStackTo(original, ExtremePatternEncoderBlockEntity.BLANK_SLOT,
                    ExtremePatternEncoderBlockEntity.BLANK_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, ExtremePatternEncoderBlockEntity.GRID_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getStatus() { return data.get(0); }
    public boolean hasValidRecipe() { return data.get(1) != 0; }
}
