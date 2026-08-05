package ru.rfvv.metatechreborn.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
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
import appeng.capabilities.Capabilities;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;
import ru.rfvv.metatechreborn.registry.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Native AE2 node for the assembler's internal extreme-pattern bank.
 *
 * <p>The attachment is registered only when AE2 is loaded, so the core block entity remains
 * usable without AE2. Forge serializes this capability together with the block entity.</p>
 */
public final class MolecularAssemblerAe2Provider implements
        ICapabilitySerializable<CompoundTag>, IInWorldGridNodeHost, ICraftingProvider, IActionHost {
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(MetaTechReborn.MOD_ID, "molecular_assembler_ae2");
    private static final Set<MolecularAssemblerAe2Provider> PROVIDERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean registered;

    private final MolecularAssemblerBlockEntity host;
    private final LazyOptional<IInWorldGridNodeHost> nodeHostCapability = LazyOptional.of(() -> this);
    private final IGridNodeListener<MolecularAssemblerAe2Provider> nodeListener =
            new IGridNodeListener<>() {
                @Override
                public void onSaveChanges(MolecularAssemblerAe2Provider owner, IGridNode node) {
                    owner.host.setChanged();
                }
            };

    private IManagedGridNode managedNode;
    private CompoundTag pendingNodeTag;
    private List<IPatternDetails> cachedPatterns = List.of();
    private List<AEItemKey> cachedDefinitions = List.of();
    private long lastPatternRefresh = Long.MIN_VALUE;
    private boolean invalid;

    private MolecularAssemblerAe2Provider(MolecularAssemblerBlockEntity host) {
        this.host = host;
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static final class Events {
        @SubscribeEvent
        public void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (!(event.getObject() instanceof MolecularAssemblerBlockEntity assembler)) return;
            MolecularAssemblerAe2Provider provider = new MolecularAssemblerAe2Provider(assembler);
            synchronized (PROVIDERS) {
                PROVIDERS.add(provider);
            }
            event.addCapability(CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
        }

        @SubscribeEvent
        public void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
            List<MolecularAssemblerAe2Provider> snapshot;
            synchronized (PROVIDERS) {
                snapshot = new ArrayList<>(PROVIDERS);
            }
            for (MolecularAssemblerAe2Provider provider : snapshot) {
                provider.tick(event.level);
            }
        }
    }

    private void tick(Level level) {
        if (invalid || host.isRemoved() || host.getLevel() != level) return;
        ensureNode();
        if (managedNode == null) return;

        long gameTime = level.getGameTime();
        if (gameTime - lastPatternRefresh >= 20L) {
            lastPatternRefresh = gameTime;
            refreshPatterns(true);
        }
        returnOutputToNetwork();
    }

    private void ensureNode() {
        if (invalid || managedNode != null) return;
        Level level = host.getLevel();
        if (level == null || level.isClientSide) return;

        managedNode = GridHelper.createManagedNode(this, nodeListener)
                .setInWorldNode(true)
                .setVisualRepresentation(ModItems.MOLECULAR_ASSEMBLER_9X9.get())
                .setIdlePowerUsage(1.0D)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .addService(ICraftingProvider.class, this);

        if (pendingNodeTag != null && !pendingNodeTag.isEmpty()) {
            managedNode.loadFromNBT(pendingNodeTag);
            pendingNodeTag = null;
        }
        managedNode.create(level, host.getBlockPos());
        refreshPatterns(false);
    }

    private void refreshPatterns(boolean notifyGrid) {
        List<IPatternDetails> patterns = new ArrayList<>();
        List<AEItemKey> definitions = new ArrayList<>();

        int slots = host.getActivePatternSlots();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack encodedStack = host.getPatternItems().getStackInSlot(slot);
            ExtremePatternData data = EncodedExtremePatternItem.read(encodedStack).orElse(null);
            if (data == null || data.output().isEmpty()) continue;

            ItemStack definitionStack = encodedStack.copy();
            definitionStack.setCount(1);
            AEItemKey definition = AEItemKey.of(definitionStack);
            if (definition == null) continue;

            patterns.add(new ExtremePatternDetails(definition, data));
            definitions.add(definition);
        }

        if (definitions.equals(cachedDefinitions)) return;
        cachedDefinitions = List.copyOf(definitions);
        cachedPatterns = List.copyOf(patterns);
        if (notifyGrid && managedNode != null && managedNode.isReady()) {
            ICraftingProvider.requestUpdate(managedNode);
        }
    }

    private void returnOutputToNetwork() {
        if (managedNode == null || !managedNode.isActive()) return;
        ItemStack output = host.getItems().getStackInSlot(MolecularAssemblerBlockEntity.OUTPUT_SLOT);
        if (output.isEmpty()) return;

        AEItemKey key = AEItemKey.of(output);
        if (key == null) return;
        var grid = managedNode.getGrid();
        if (grid == null) return;

        long inserted = grid.getStorageService().getInventory().insert(
                key,
                output.getCount(),
                Actionable.MODULATE,
                IActionSource.ofMachine(this)
        );
        if (inserted > 0) {
            host.getItems().extractItem(
                    MolecularAssemblerBlockEntity.OUTPUT_SLOT,
                    (int) Math.min(Integer.MAX_VALUE, inserted),
                    false
            );
            host.setChanged();
        }
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        refreshPatterns(false);
        return cachedPatterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder) {
        if (pattern == null || isBusy()) return false;
        refreshPatterns(false);

        IPatternDetails published = null;
        for (IPatternDetails candidate : cachedPatterns) {
            if (candidate.getDefinition().equals(pattern.getDefinition())) {
                published = candidate;
                break;
            }
        }
        if (published == null) return false;

        GenericStack output = published.getPrimaryOutput();
        if (output == null || !(output.what() instanceof AEItemKey outputKey)) return false;

        List<ItemStack> supplied = collectItemInputs(inputHolder);
        if (supplied == null) return false;

        if (!host.acceptExternalPatternBatch(supplied, outputKey.toStack(1), output.amount())) {
            return false;
        }
        for (KeyCounter counter : inputHolder) counter.clear();
        return true;
    }

    private static @Nullable List<ItemStack> collectItemInputs(KeyCounter[] inputHolder) {
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

    @Override
    public boolean isBusy() {
        return !host.canAcceptAe2Plan();
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction direction) {
        ensureNode();
        return managedNode == null ? null : managedNode.getNode();
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        ensureNode();
        return managedNode == null ? null : managedNode.getNode();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable Direction side) {
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) return nodeHostCapability.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (managedNode != null) {
            managedNode.saveToNBT(tag);
        } else if (pendingNodeTag != null) {
            tag.merge(pendingNodeTag.copy());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        pendingNodeTag = tag.copy();
    }

    private void invalidate() {
        if (invalid) return;
        invalid = true;
        synchronized (PROVIDERS) {
            PROVIDERS.remove(this);
        }
        if (managedNode != null) {
            managedNode.destroy();
            managedNode = null;
        }
        nodeHostCapability.invalidate();
    }

    private static final class ExtremePatternDetails implements IPatternDetails {
        private final AEItemKey definition;
        private final IInput[] inputs;
        private final GenericStack[] outputs;

        private ExtremePatternDetails(AEItemKey definition, ExtremePatternData data) {
            this.definition = definition;

            Map<AEItemKey, Long> inputAmounts = new LinkedHashMap<>();
            for (ItemStack stack : data.inputs()) {
                if (stack.isEmpty()) continue;
                AEItemKey key = AEItemKey.of(stack);
                if (key != null) inputAmounts.merge(key, (long) stack.getCount(), Long::sum);
            }
            this.inputs = inputAmounts.entrySet().stream()
                    .map(entry -> new ExactInput(entry.getKey(), entry.getValue()))
                    .toArray(IInput[]::new);

            AEItemKey outputKey = AEItemKey.of(data.output());
            this.outputs = outputKey == null
                    ? new GenericStack[0]
                    : new GenericStack[]{new GenericStack(outputKey, data.output().getCount())};
        }

        @Override
        public AEItemKey getDefinition() {
            return definition;
        }

        @Override
        public IInput[] getInputs() {
            return inputs;
        }

        @Override
        public GenericStack[] getOutputs() {
            return outputs;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof ExtremePatternDetails details
                    && definition.equals(details.definition);
        }

        @Override
        public int hashCode() {
            return definition.hashCode();
        }
    }

    private record ExactInput(AEItemKey key, long multiplier) implements IPatternDetails.IInput {
        @Override
        public GenericStack[] getPossibleInputs() {
            return new GenericStack[]{new GenericStack(key, 1)};
        }

        @Override
        public long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean isValid(AEKey candidate, Level level) {
            return key.equals(candidate);
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
