package ru.rfvv.metatechreborn.pattern;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record MysticalInfusionPatternData(ResourceLocation recipeId,
                                           NonNullList<ItemStack> inputs,
                                           ItemStack output) {
    public static final int SLOT_COUNT = 9;
    private static final int VERSION = 1;

    public MysticalInfusionPatternData {
        if (recipeId == null) throw new IllegalArgumentException("recipeId");
        if (inputs.size() != SLOT_COUNT) throw new IllegalArgumentException("Mystical infusion needs 9 input slots");
        NonNullList<ItemStack> safe = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inputs.get(i);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                safe.set(i, copy);
            }
        }
        inputs = safe;
        output = output.copy();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", VERSION);
        tag.putString("Recipe", recipeId.toString());
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inputs.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            entry.put("Stack", stack.save(new CompoundTag()));
            list.add(entry);
        }
        tag.put("Inputs", list);
        tag.put("Output", output.save(new CompoundTag()));
        return tag;
    }

    public static Optional<MysticalInfusionPatternData> load(CompoundTag tag) {
        if (tag == null || tag.getInt("Version") != VERSION) return Optional.empty();
        ResourceLocation recipe = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (recipe == null || !tag.contains("Output", Tag.TAG_COMPOUND)) return Optional.empty();
        NonNullList<ItemStack> inputs = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ListTag list = tag.getList("Inputs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < SLOT_COUNT && entry.contains("Stack", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                    inputs.set(slot, stack);
                }
            }
        }
        ItemStack output = ItemStack.of(tag.getCompound("Output"));
        if (output.isEmpty()) return Optional.empty();
        return Optional.of(new MysticalInfusionPatternData(recipe, inputs, output));
    }

    public NonNullList<ItemStack> copyInputs() {
        NonNullList<ItemStack> result = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) result.set(i, inputs.get(i).copy());
        return result;
    }
}
