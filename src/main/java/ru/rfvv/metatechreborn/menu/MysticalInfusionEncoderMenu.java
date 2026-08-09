package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionEncoderBlockEntity;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

public final class MysticalInfusionEncoderMenu extends AbstractContainerMenu {
    public static final int ENCODE_BUTTON = 0;
    public static final int CLEAR_BUTTON = 1;
    public static final int PLAYER_START = MysticalInfusionEncoderBlockEntity.TOTAL_SLOTS;

    private final MysticalInfusionEncoderBlockEntity blockEntity;
    private final ContainerData data;

    public MysticalInfusionEncoderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (MysticalInfusionEncoderBlockEntity) inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(2));
    }

    public MysticalInfusionEncoderMenu(int id, Inventory inventory,
                                       MysticalInfusionEncoderBlockEntity blockEntity, ContainerData data) {
        super(ModMysticalInfusion.ENCODER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        int[] map = {0,1,2,3,5,6,7,8};
        addGhost(0, 78, 56); // central altar item
        for (int i = 0; i < 8; i++) {
            int grid = map[i];
            addGhost(i + 1, 42 + (grid % 3) * 36, 20 + (grid / 3) * 36);
        }
        addSlot(new SlotItemHandler(blockEntity.getItems(), MysticalInfusionEncoderBlockEntity.BLANK_SLOT, 184, 38));
        addSlot(new SlotItemHandler(blockEntity.getItems(), MysticalInfusionEncoderBlockEntity.ENCODED_SLOT, 256, 38) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(blockEntity.getItems(), MysticalInfusionEncoderBlockEntity.PREVIEW_SLOT, 148, 56) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(@NotNull Player player) { return false; }
        });

        int invX = 78, invY = 142;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
        addDataSlots(data);
    }

    private void addGhost(int slot, int x, int y) {
        addSlot(new SlotItemHandler(blockEntity.getItems(), slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            @Override public boolean mayPickup(@NotNull Player player) { return false; }
        });
    }

    @Override public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModMysticalInfusion.ENCODER.get());
    }

    @Override public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == ENCODE_BUTTON) return blockEntity.encode();
        if (id == CLEAR_BUTTON) { blockEntity.clearRecipe(); return true; }
        return false;
    }

    public boolean applyRecipe(ResourceLocation id) { return blockEntity.selectRecipe(id); }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModMysticalInfusion.BLANK_PATTERN.get())) {
            if (!moveItemStackTo(stack, MysticalInfusionEncoderBlockEntity.BLANK_SLOT,
                    MysticalInfusionEncoderBlockEntity.BLANK_SLOT + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }

    public int getStatus() { return data.get(0); }
    public boolean hasRecipe() { return data.get(1) != 0; }
}
