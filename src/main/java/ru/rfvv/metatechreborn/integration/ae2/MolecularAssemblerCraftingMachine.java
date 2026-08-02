package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.capabilities.Capabilities;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** AE2 crafting provider/machine bridge for the native MetaTech 9x9 assembler. */
public final class MolecularAssemblerCraftingMachine implements ICraftingMachine,
        ICraftingProvider, IInWorldGridNodeHost, IActionHost,
        IGridNodeListener<MolecularAssemblerBlockEntity> {
    private static final Map<MolecularAssemblerBlockEntity, MolecularAssemblerCraftingMachine> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean DECODER_REGISTERED = new AtomicBoolean();

    private final MolecularAssemblerBlockEntity host;
    private final IManagedGridNode managedNode;
    private final LazyOptional<ICraftingMachine> craftingMachineCapability;
    private final LazyOptional<IInWorldGridNodeHost> nodeHostCapability;

    private MolecularAssemblerCraftingMachine(MolecularAssemblerBlockEntity host) {
        this.host = host;
        this.managedNode = GridHelper.createManagedNode(host, this)
                .setTagName("MetaTechAssemblerNode")
                .setInWorldNode(true)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(2.0D)
                .setVisualRepresentation(ModItems.MOLECULAR_ASSEMBLER_9X9.get())
                .addService(ICraftingProvider.class, this);
        this.craftingMachineCapability = LazyOptional.of(() -> this);
        this.nodeHostCapability = LazyOptional.of(() -> this);
    }

    public static void bootstrap() {
        if (DECODER_REGISTERED.compareAndSet(false, true)) {
            PatternDetailsHelper.registerDecoder(ExtremePatternDetails.Decoder.INSTANCE);
            MetaTechReborn.LOGGER.info("Registered MetaTech 9x9 pattern decoder with AE2");
        }
    }

    private static MolecularAssemblerCraftingMachine get(MolecularAssemblerBlockEntity host) {
        return INSTANCES.computeIfAbsent(host, MolecularAssemblerCraftingMachine::new);
    }

    public static void clearRemoved(MolecularAssemblerBlockEntity host) {
        MolecularAssemblerCraftingMachine integration = get(host);
        GridHelper.onFirstTick(host, ignored -> {
            if (!integration.managedNode.isReady() && host.getLevel() != null) {
                integration.managedNode.create(host.getLevel(), host.getBlockPos());
                ICraftingProvider.requestUpdate(integration.managedNode);
            }
        });
    }

    public static void loadNode(MolecularAssemblerBlockEntity host, CompoundTag tag) {
        get(host).managedNode.loadFromNBT(tag);
    }

    public static void saveNode(MolecularAssemblerBlockEntity host, CompoundTag tag) {
        MolecularAssemblerCraftingMachine integration = INSTANCES.get(host);
        if (integration != null) integration.managedNode.saveToNBT(tag);
    }

    public static void remove(MolecularAssemblerBlockEntity host) {
        MolecularAssemblerCraftingMachine integration = INSTANCES.remove(host);
        if (integration == null) return;
        integration.craftingMachineCapability.invalidate();
        integration.nodeHostCapability.invalidate();
        integration.managedNode.destroy();
    }

    public static void requestPatternUpdate(MolecularAssemblerBlockEntity host) {
        MolecularAssemblerCraftingMachine integration = INSTANCES.get(host);
        if (integration != null && integration.managedNode.isReady()) {
            ICraftingProvider.requestUpdate(integration.managedNode);
        }
    }

    public static int insertIntoNetwork(MolecularAssemblerBlockEntity host, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        MolecularAssemblerCraftingMachine integration = INSTANCES.get(host);
        if (integration == null || !integration.managedNode.isOnline()) return 0;
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return 0;
        var grid = integration.managedNode.getGrid();
        if (grid == null) return 0;
        long inserted = grid.getStorageService().getInventory().insert(
                key, stack.getCount(), Actionable.MODULATE, IActionSource.ofMachine(integration));
        return (int) Math.min(Integer.MAX_VALUE, inserted);
    }

    public static <T> @Nullable LazyOptional<T> getCapability(
            MolecularAssemblerBlockEntity host, Capability<T> requested) {
        MolecularAssemblerCraftingMachine integration = get(host);
        if (requested == Capabilities.CRAFTING_MACHINE) return integration.craftingMachineCapability.cast();
        if (requested == Capabilities.IN_WORLD_GRID_NODE_HOST) return integration.nodeHostCapability.cast();
        return null;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(
                AEItemKey.of(ModItems.MOLECULAR_ASSEMBLER_9X9.get()),
                Component.translatable("container.metatech_reborn.molecular_assembler_9x9"),
                List.of(
                        Component.translatable("gui.metatech_reborn.ae2_native_patterns"),
                        Component.translatable("gui.metatech_reborn.assembler.queue",
                                host.getQueuedJobCount(), 64),
                        Component.translatable("gui.metatech_reborn.assembler.speed_count",
                                host.getSpeedCardCount(), MolecularAssemblerBlockEntity.SPEED_CARD_SLOTS)
                )
        );
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder, Direction direction) {
        return pushPattern(pattern, inputHolder);
    }

    @Override public boolean acceptsPlans() { return host.canAcceptAe2Plan(); }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        List<IPatternDetails> result = new ArrayList<>();
        for (int slot = 0; slot < host.getActivePatternSlots(); slot++) {
            ItemStack stack = host.getPatternItems().getStackInSlot(slot);
            ExtremePatternDetails details = ExtremePatternDetails.decode(stack);
            if (details != null) result.add(details);
        }
        return List.copyOf(result);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!host.canAcceptAe2Plan()) return false;

        if (patternDetails instanceof ExtremePatternDetails extreme) {
            boolean installed = getAvailablePatterns().stream().anyMatch(extreme::equals);
            if (!installed) return false;
            NonNullList<ItemStack> placement = createAtomicPlacement(extreme, inputHolder);
            return placement != null && host.acceptDecodedAe2Pattern(extreme.pattern(), placement);
        }

        List<ItemStack> supplied = flattenInputs(inputHolder);
        if (supplied == null) return false;
        for (GenericStack output : patternDetails.getOutputs()) {
            if (output == null || !(output.what() instanceof AEItemKey itemKey)) continue;
            ItemStack requested = itemKey.toStack(1);
            if (host.acceptExternalPatternBatch(supplied, requested, output.amount())) return true;
        }
        return false;
    }

    private @Nullable NonNullList<ItemStack> createAtomicPlacement(
            ExtremePatternDetails pattern, KeyCounter[] inputHolder) {
        IPatternDetails.IInput[] expectedInputs = pattern.getInputs();
        if (inputHolder.length != expectedInputs.length || host.getLevel() == null) return null;

        NonNullList<ItemStack> placement = NonNullList.withSize(
                MolecularAssemblerBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        for (int inputIndex = 0; inputIndex < inputHolder.length; inputIndex++) {
            AEItemKey selectedKey = null;
            long selectedAmount = 0;
            for (Object2LongMap.Entry<AEKey> entry : inputHolder[inputIndex]) {
                if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0) return null;
                if (!expectedInputs[inputIndex].isValid(itemKey, host.getLevel())) return null;
                if (selectedKey != null && !selectedKey.equals(itemKey)) return null;
                selectedKey = itemKey;
                selectedAmount += entry.getLongValue();
            }
            if (selectedKey == null || selectedAmount != expectedInputs[inputIndex].getMultiplier()) return null;
            placement.set(pattern.getGridSlotForInput(inputIndex), selectedKey.toStack((int) selectedAmount));
        }
        return placement;
    }

    private static @Nullable List<ItemStack> flattenInputs(KeyCounter[] inputHolder) {
        List<ItemStack> supplied = new ArrayList<>();
        for (KeyCounter counter : inputHolder) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey)) return null;
                long amount = entry.getLongValue();
                while (amount > 0) {
                    int batch = (int) Math.min(itemKey.getMaxStackSize(), amount);
                    supplied.add(itemKey.toStack(batch));
                    amount -= batch;
                }
            }
        }
        return supplied;
    }

    @Override public boolean isBusy() { return host.isAe2Busy(); }
    @Override public @Nullable IGridNode getGridNode(Direction dir) { return managedNode.getNode(); }
    @Override public @Nullable IGridNode getActionableNode() { return managedNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction dir) { return AECableType.SMART; }
    @Override public void onSaveChanges(MolecularAssemblerBlockEntity owner, IGridNode node) { owner.setChanged(); }
    @Override public void onGridChanged(MolecularAssemblerBlockEntity owner, IGridNode node) { owner.setChanged(); }
    @Override public void onStateChanged(MolecularAssemblerBlockEntity owner, IGridNode node, State state) {
        owner.setChanged();
    }
}
