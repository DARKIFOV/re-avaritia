package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.integration.mystical.MysticalInfusionSupport;
import ru.rfvv.metatechreborn.item.EncodedMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.menu.MysticalInfusionEncoderMenu;
import ru.rfvv.metatechreborn.pattern.MysticalInfusionPatternData;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

import java.util.Optional;

public final class MysticalInfusionEncoderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GHOST_START = 0;
    public static final int GHOST_COUNT = 9;
    public static final int BLANK_SLOT = 9;
    public static final int ENCODED_SLOT = 10;
    public static final int PREVIEW_SLOT = 11;
    public static final int TOTAL_SLOTS = 12;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_ENCODED = 2;
    public static final int STATUS_NO_BLANK = 3;
    public static final int STATUS_OUTPUT_BLOCKED = 4;
    public static final int STATUS_NO_RECIPE = 5;

    private ResourceLocation selectedRecipe;
    private int status = STATUS_IDLE;
    private boolean updating;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            if (!updating && slot == BLANK_SLOT) status = selectedRecipe == null ? STATUS_IDLE : STATUS_READY;
            setChanged();
        }
        @Override public int getSlotLimit(int slot) { return slot == BLANK_SLOT ? 64 : 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == BLANK_SLOT && stack.is(ModMysticalInfusion.BLANK_PATTERN.get());
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return index == 0 ? status : index == 1 && selectedRecipe != null ? 1 : 0;
        }
        @Override public void set(int index, int value) { if (index == 0) status = value; }
        @Override public int getCount() { return 2; }
    };

    public MysticalInfusionEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModMysticalInfusion.ENCODER_BE.get(), pos, state);
    }

    public boolean selectRecipe(ResourceLocation id) {
        if (level == null || level.isClientSide) return false;
        Optional<MysticalInfusionSupport.View> found = MysticalInfusionSupport.find(level, id);
        if (found.isEmpty()) return false;
        var view = found.get();
        NonNullList<ItemStack> display = view.displayInputs();
        updating = true;
        try {
            for (int i = 0; i < GHOST_COUNT; i++) items.setStackInSlot(i, display.get(i));
            items.setStackInSlot(PREVIEW_SLOT, view.output().copy());
        } finally {
            updating = false;
        }
        selectedRecipe = id;
        status = STATUS_READY;
        setChanged();
        return true;
    }

    public void clearRecipe() {
        selectedRecipe = null;
        updating = true;
        try {
            for (int i = 0; i < GHOST_COUNT; i++) items.setStackInSlot(i, ItemStack.EMPTY);
            items.setStackInSlot(PREVIEW_SLOT, ItemStack.EMPTY);
        } finally {
            updating = false;
        }
        status = STATUS_IDLE;
        setChanged();
    }

    public boolean encode() {
        if (level == null || level.isClientSide || selectedRecipe == null) {
            status = STATUS_NO_RECIPE;
            return false;
        }
        if (!items.getStackInSlot(ENCODED_SLOT).isEmpty()) {
            status = STATUS_OUTPUT_BLOCKED;
            return false;
        }
        ItemStack blank = items.getStackInSlot(BLANK_SLOT);
        if (blank.isEmpty()) {
            status = STATUS_NO_BLANK;
            return false;
        }
        Optional<MysticalInfusionSupport.View> found = MysticalInfusionSupport.find(level, selectedRecipe);
        if (found.isEmpty()) {
            status = STATUS_NO_RECIPE;
            return false;
        }
        var view = found.get();
        MysticalInfusionPatternData pattern = new MysticalInfusionPatternData(
                view.id(), view.displayInputs(), view.output());
        blank.shrink(1);
        items.setStackInSlot(BLANK_SLOT, blank);
        items.setStackInSlot(ENCODED_SLOT, EncodedMysticalInfusionPatternItem.create(pattern));
        status = STATUS_ENCODED;
        setChanged();
        return true;
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }
    public @Nullable ResourceLocation getSelectedRecipe() { return selectedRecipe; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> result = NonNullList.create();
        for (int slot : new int[]{BLANK_SLOT, ENCODED_SLOT}) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) result.add(stack.copy());
        }
        return result;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.literal("Кодировщик мистического алтаря");
    }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                                 @NotNull Player player) {
        return new MysticalInfusionEncoderMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Status", status);
        if (selectedRecipe != null) tag.putString("Recipe", selectedRecipe.toString());
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) items.deserializeNBT(tag.getCompound("Inventory"));
        status = tag.getInt("Status");
        selectedRecipe = tag.contains("Recipe", Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString("Recipe")) : null;
    }
}
