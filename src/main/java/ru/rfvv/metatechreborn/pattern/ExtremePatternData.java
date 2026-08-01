package ru.rfvv.metatechreborn.pattern;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Native MetaTech representation of one complete 9x9 crafting pattern.
 *
 * The format is intentionally independent from the temporary companion mod so
 * existing encoded patterns can later be consumed directly by the MetaTech
 * assembler and terminal.
 */
public record ExtremePatternData(NonNullList<ItemStack> inputs, ItemStack output) {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 9;
    public static final int SLOT_COUNT = WIDTH * HEIGHT;
    public static final int FORMAT_VERSION = 1;

    private static final String TAG_VERSION = "FormatVersion";
    private static final String TAG_INPUTS = "Inputs";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final String TAG_OUTPUT = "Output";

    public ExtremePatternData {
        if (inputs.size() != SLOT_COUNT) {
            throw new IllegalArgumentException("A 9x9 pattern must contain exactly " + SLOT_COUNT + " slots");
        }
        NonNullList<ItemStack> safeInputs = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inputs.get(slot);
            safeInputs.set(slot, normalizedCopy(stack));
        }
        inputs = safeInputs;
        output = output.copy();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_VERSION, FORMAT_VERSION);

        ListTag inputList = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inputs.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte(TAG_SLOT, (byte) slot);
            entry.put(TAG_STACK, stack.save(new CompoundTag()));
            inputList.add(entry);
        }
        tag.put(TAG_INPUTS, inputList);
        tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        return tag;
    }

    public static Optional<ExtremePatternData> load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return Optional.empty();
        if (tag.getInt(TAG_VERSION) != FORMAT_VERSION) return Optional.empty();
        if (!tag.contains(TAG_OUTPUT, Tag.TAG_COMPOUND)) return Optional.empty();

        NonNullList<ItemStack> inputs = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ListTag inputList = tag.getList(TAG_INPUTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < inputList.size(); index++) {
            CompoundTag entry = inputList.getCompound(index);
            int slot = entry.getByte(TAG_SLOT) & 0xFF;
            if (slot < 0 || slot >= SLOT_COUNT || !entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) continue;
            inputs.set(slot, normalizedCopy(ItemStack.of(entry.getCompound(TAG_STACK))));
        }

        ItemStack output = ItemStack.of(tag.getCompound(TAG_OUTPUT));
        if (output.isEmpty()) return Optional.empty();
        return Optional.of(new ExtremePatternData(inputs, output));
    }

    public int ingredientCount() {
        int count = 0;
        for (ItemStack input : inputs) {
            if (!input.isEmpty()) count++;
        }
        return count;
    }

    public NonNullList<ItemStack> copyInputs() {
        NonNullList<ItemStack> copy = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOT_COUNT; slot++) copy.set(slot, inputs.get(slot).copy());
        return copy;
    }

    private static ItemStack normalizedCopy(ItemStack source) {
        if (source == null || source.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = source.copy();
        copy.setCount(1);
        return copy;
    }
}
