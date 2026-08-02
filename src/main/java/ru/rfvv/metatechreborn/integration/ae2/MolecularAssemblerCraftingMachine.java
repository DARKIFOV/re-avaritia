package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Direct AE2 Pattern Provider integration for the native MetaTech 9x9 pattern bank. */
public final class MolecularAssemblerCraftingMachine implements ICraftingMachine {
    private static final Map<MolecularAssemblerBlockEntity, MolecularAssemblerCraftingMachine> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MolecularAssemblerBlockEntity host;
    private final LazyOptional<ICraftingMachine> capability = LazyOptional.of(() -> this);

    private MolecularAssemblerCraftingMachine(MolecularAssemblerBlockEntity host) {
        this.host = host;
    }

    public static <T> @Nullable LazyOptional<T> getCapability(
            MolecularAssemblerBlockEntity host, Capability<T> requested) {
        if (requested != Capabilities.CRAFTING_MACHINE) return null;
        MolecularAssemblerCraftingMachine machine = INSTANCES.computeIfAbsent(
                host, MolecularAssemblerCraftingMachine::new);
        return machine.capability.cast();
    }

    public static void invalidate(MolecularAssemblerBlockEntity host) {
        MolecularAssemblerCraftingMachine machine = INSTANCES.remove(host);
        if (machine != null) machine.capability.invalidate();
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(ModItems.MOLECULAR_ASSEMBLER_9X9.get()),
                Component.translatable("container.metatech_reborn.molecular_assembler_9x9"),
                List.of(Component.translatable("gui.metatech_reborn.ae2_native_patterns"))
        );
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder, Direction direction) {
        if (!acceptsPlans()) return false;

        List<ItemStack> supplied = new ArrayList<>();
        for (KeyCounter counter : inputHolder) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey)) return false;
                long amount = entry.getLongValue();
                while (amount > 0) {
                    int batch = (int) Math.min(itemKey.getMaxStackSize(), amount);
                    supplied.add(itemKey.toStack(batch));
                    amount -= batch;
                }
            }
        }

        for (GenericStack output : pattern.getOutputs()) {
            if (output == null || !(output.what() instanceof AEItemKey itemKey)) continue;
            ItemStack requested = itemKey.toStack(1);
            if (host.acceptExternalPatternBatch(supplied, requested, output.amount())) {
                // AE2 transfers ownership of these inputs only when pushPattern succeeds.
                // The host validated and placed the whole batch atomically, so consume the
                // original counters after acceptance rather than leaving ghost ingredients.
                for (KeyCounter counter : inputHolder) counter.clear();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptsPlans() {
        return host.canAcceptAe2Plan();
    }
}
