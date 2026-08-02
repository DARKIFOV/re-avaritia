package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Official AE2 representation of a native MetaTech 9x9 pattern. */
public final class ExtremePatternDetails implements IPatternDetails {
    private final AEItemKey definition;
    private final ExtremePatternData pattern;
    private final IInput[] inputs;
    private final GenericStack[] outputs;
    private final int[] inputGridSlots;

    private ExtremePatternDetails(ItemStack definitionStack, ExtremePatternData pattern) {
        ItemStack normalizedDefinition = definitionStack.copy();
        normalizedDefinition.setCount(1);
        this.definition = Objects.requireNonNull(AEItemKey.of(normalizedDefinition), "pattern definition");
        this.pattern = pattern;

        List<IInput> decodedInputs = new ArrayList<>();
        List<Integer> decodedSlots = new ArrayList<>();
        for (int slot = 0; slot < ExtremePatternData.SLOT_COUNT; slot++) {
            ItemStack stack = pattern.inputs().get(slot);
            if (stack.isEmpty()) continue;
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) continue;
            decodedInputs.add(new ExactInput(key));
            decodedSlots.add(slot);
        }
        this.inputs = decodedInputs.toArray(IInput[]::new);
        this.inputGridSlots = decodedSlots.stream().mapToInt(Integer::intValue).toArray();

        AEItemKey outputKey = Objects.requireNonNull(AEItemKey.of(pattern.output()), "pattern output");
        this.outputs = new GenericStack[] { new GenericStack(outputKey, pattern.output().getCount()) };
    }

    public static @Nullable ExtremePatternDetails decode(ItemStack stack) {
        return EncodedExtremePatternItem.read(stack)
                .map(pattern -> new ExtremePatternDetails(stack, pattern))
                .orElse(null);
    }

    public ExtremePatternData pattern() { return pattern; }
    public int getGridSlotForInput(int inputIndex) { return inputGridSlots[inputIndex]; }

    @Override public AEItemKey getDefinition() { return definition; }
    @Override public IInput[] getInputs() { return inputs; }
    @Override public GenericStack[] getOutputs() { return outputs; }
    @Override public boolean supportsPushInputsToExternalInventory() { return false; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ExtremePatternDetails details
                && definition.equals(details.definition);
    }

    @Override public int hashCode() { return definition.hashCode(); }

    @Override
    public String toString() {
        return "ExtremePatternDetails{" + definition + ", slots=" + Arrays.toString(inputGridSlots) + "}";
    }

    private record ExactInput(AEItemKey key) implements IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[] { new GenericStack(key, 1) };
        }

        @Override public long getMultiplier() { return 1; }
        @Override public boolean isValid(AEKey input, Level level) { return key.equals(input); }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            if (!(template instanceof AEItemKey itemKey)) return null;
            ItemStack input = itemKey.toStack();
            if (!input.hasCraftingRemainingItem()) return null;
            ItemStack remainder = input.getCraftingRemainingItem();
            return remainder.isEmpty() ? null : AEItemKey.of(remainder);
        }
    }

    public enum Decoder implements IPatternDetailsDecoder {
        INSTANCE;

        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return EncodedExtremePatternItem.read(stack).isPresent();
        }

        @Override
        public @Nullable IPatternDetails decodePattern(AEItemKey what, Level level) {
            return decodePattern(what.toStack(), level, false);
        }

        @Override
        public @Nullable IPatternDetails decodePattern(ItemStack what, Level level, boolean tryRecovery) {
            return ExtremePatternDetails.decode(what);
        }
    }
}
