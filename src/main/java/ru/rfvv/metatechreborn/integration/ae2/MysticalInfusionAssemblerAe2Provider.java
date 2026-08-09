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
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionAssemblerBlockEntity;
import ru.rfvv.metatechreborn.item.EncodedMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.pattern.MysticalInfusionPatternData;
import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MysticalInfusionAssemblerAe2Provider implements
        ICapabilitySerializable<CompoundTag>, IInWorldGridNodeHost, ICraftingProvider, IActionHost {
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(MetaTechReborn.MOD_ID, "mystical_infusion_assembler_ae2");
    private static final Set<MysticalInfusionAssemblerAe2Provider> PROVIDERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean registered;

    private final MysticalInfusionAssemblerBlockEntity host;
    private final LazyOptional<IInWorldGridNodeHost> nodeHostCapability = LazyOptional.of(() -> this);
    private final IGridNodeListener<MysticalInfusionAssemblerAe2Provider> listener = new IGridNodeListener<>() {
        @Override public void onSaveChanges(MysticalInfusionAssemblerAe2Provider owner, IGridNode node) {
            owner.host.setChanged();
        }
        @Override public void onInWorldConnectionChanged(MysticalInfusionAssemblerAe2Provider owner, IGridNode node) {
            owner.refreshPending = true;
        }
        @Override public void onGridChanged(MysticalInfusionAssemblerAe2Provider owner, IGridNode node) {
            owner.refreshPending = true;
        }
        @Override public void onStateChanged(MysticalInfusionAssemblerAe2Provider owner, IGridNode node,
                                             IGridNodeListener.State state) {
            owner.refreshPending = true;
        }
    };

    private IManagedGridNode managedNode;
    private CompoundTag pendingNodeTag;
    private List<IPatternDetails> cachedPatterns = List.of();
    private List<AEItemKey> cachedDefinitions = List.of();
    private long lastPatternRefresh = -20L;
    private long lastCraftingRefresh = -200L;
    private boolean refreshPending = true;
    private boolean invalid;

    private MysticalInfusionAssemblerAe2Provider(MysticalInfusionAssemblerBlockEntity host) { this.host = host; }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(new MysticalInfusionEvents());
    }

    private static final class MysticalInfusionEvents {
        @SubscribeEvent public void attach(AttachCapabilitiesEvent<BlockEntity> event) {
            if (!(event.getObject() instanceof MysticalInfusionAssemblerBlockEntity assembler)) return;
            var provider = new MysticalInfusionAssemblerAe2Provider(assembler);
            synchronized (PROVIDERS) { PROVIDERS.add(provider); }
            event.addCapability(CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
        }
        @SubscribeEvent public void tick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
            List<MysticalInfusionAssemblerAe2Provider> snapshot;
            synchronized (PROVIDERS) { snapshot = new ArrayList<>(PROVIDERS); }
            for (var provider : snapshot) provider.tickServer(event.level);
        }
    }

    private void tickServer(Level level) {
        if (invalid || host.isRemoved() || host.getLevel() != level) return;
        ensureNode();
        if (managedNode == null) return;
        long time = level.getGameTime();
        if (time - lastPatternRefresh >= 20L) {
            lastPatternRefresh = time;
            connectAdjacent(level);
            refreshPatterns(true);
        }
        if (time - lastCraftingRefresh >= 200L) refreshPending = true;
        requestRefresh();
        returnOutput();
    }

    private void ensureNode() {
        if (invalid || managedNode != null || host.getLevel() == null || host.getLevel().isClientSide) return;
        managedNode = GridHelper.createManagedNode(this, listener)
                .setInWorldNode(true)
                .setVisualRepresentation(ModMysticalInfusion.ASSEMBLER_ITEM.get())
                .setIdlePowerUsage(1.0D)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .addService(ICraftingProvider.class, this);
        if (pendingNodeTag != null && !pendingNodeTag.isEmpty()) {
            managedNode.loadFromNBT(pendingNodeTag); pendingNodeTag = null;
        }
        managedNode.create(host.getLevel(), host.getBlockPos());
        refreshPatterns(false); refreshPending = true;
    }

    private void connectAdjacent(Level level) {
        if (managedNode == null || managedNode.getNode() == null) return;
        IGridNode own = managedNode.getNode();
        for (Direction direction : Direction.values()) {
            IGridNode adjacent = GridHelper.getExposedNode(level, host.getBlockPos().relative(direction), direction.getOpposite());
            if (adjacent == null || adjacent == own) continue;
            try { GridHelper.createConnection(own, adjacent); } catch (IllegalStateException ignored) {}
        }
    }

    private void refreshPatterns(boolean notify) {
        List<IPatternDetails> patterns = new ArrayList<>();
        List<AEItemKey> definitions = new ArrayList<>();
        for (int slot = 0; slot < host.getActivePatternSlots(); slot++) {
            ItemStack encoded = host.getPatternItems().getStackInSlot(slot);
            MysticalInfusionPatternData data = EncodedMysticalInfusionPatternItem.read(encoded).orElse(null);
            if (data == null || data.output().isEmpty()) continue;
            ItemStack definitionStack = encoded.copy(); definitionStack.setCount(1);
            AEItemKey definition = AEItemKey.of(definitionStack);
            if (definition == null) continue;
            patterns.add(new MysticalPatternDetails(definition, data));
            definitions.add(definition);
        }
        if (definitions.equals(cachedDefinitions)) return;
        cachedDefinitions = List.copyOf(definitions);
        cachedPatterns = List.copyOf(patterns);
        refreshPending = true;
        if (notify) requestRefresh();
    }

    private void requestRefresh() {
        if (!refreshPending || managedNode == null || !managedNode.isReady()
                || !managedNode.isActive() || !managedNode.hasGridBooted()) return;
        ICraftingProvider.requestUpdate(managedNode);
        refreshPending = false;
        if (host.getLevel() != null) lastCraftingRefresh = host.getLevel().getGameTime();
    }

    private void returnOutput() {
        if (managedNode == null || !managedNode.isActive()) return;
        ItemStack output = host.getItems().getStackInSlot(MysticalInfusionAssemblerBlockEntity.OUTPUT_SLOT);
        if (output.isEmpty()) return;
        AEItemKey key = AEItemKey.of(output);
        if (key == null || managedNode.getGrid() == null) return;
        long inserted = managedNode.getGrid().getStorageService().getInventory().insert(
                key, output.getCount(), Actionable.MODULATE, IActionSource.ofMachine(this));
        if (inserted > 0) {
            host.getItems().extractItem(MysticalInfusionAssemblerBlockEntity.OUTPUT_SLOT,
                    (int) Math.min(Integer.MAX_VALUE, inserted), false);
            host.setChanged();
        }
    }

    @Override public List<IPatternDetails> getAvailablePatterns() { refreshPatterns(false); return cachedPatterns; }

    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder) {
        if (pattern == null || isBusy()) return false;
        refreshPatterns(false);
        MysticalPatternDetails published = null;
        for (IPatternDetails candidate : cachedPatterns) {
            if (candidate.getDefinition().equals(pattern.getDefinition()) && candidate instanceof MysticalPatternDetails details) {
                published = details; break;
            }
        }
        if (published == null) return false;
        GenericStack output = published.getPrimaryOutput();
        if (output == null || !(output.what() instanceof AEItemKey outputKey)) return false;
        List<ItemStack> supplied = collectInputs(inputHolder);
        if (supplied == null || !host.acceptExternalPatternBatch(
                published.data, supplied, outputKey.toStack(1), output.amount())) return false;
        for (KeyCounter counter : inputHolder) counter.clear();
        return true;
    }

    private static @Nullable List<ItemStack> collectInputs(KeyCounter[] holders) {
        List<ItemStack> result = new ArrayList<>();
        for (KeyCounter counter : holders) for (Object2LongMap.Entry<AEKey> entry : counter) {
            if (!(entry.getKey() instanceof AEItemKey key)) return null;
            long amount = entry.getLongValue();
            while (amount > 0) {
                int batch = (int) Math.min(key.getMaxStackSize(), amount);
                result.add(key.toStack(batch)); amount -= batch;
            }
        }
        return result;
    }

    @Override public boolean isBusy() { return !host.canAcceptAe2Plan(); }
    @Override public @Nullable IGridNode getGridNode(Direction direction) { ensureNode(); return managedNode == null ? null : managedNode.getNode(); }
    @Override public @Nullable IGridNode getActionableNode() { ensureNode(); return managedNode == null ? null : managedNode.getNode(); }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == Capabilities.IN_WORLD_GRID_NODE_HOST ? nodeHostCapability.cast() : LazyOptional.empty();
    }
    @Override public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (managedNode != null) managedNode.saveToNBT(tag); else if (pendingNodeTag != null) tag.merge(pendingNodeTag.copy());
        return tag;
    }
    @Override public void deserializeNBT(CompoundTag tag) { pendingNodeTag = tag.copy(); }
    private void invalidate() {
        if (invalid) return; invalid = true;
        synchronized (PROVIDERS) { PROVIDERS.remove(this); }
        if (managedNode != null) { managedNode.destroy(); managedNode = null; }
        nodeHostCapability.invalidate();
    }

    private static final class MysticalPatternDetails implements IPatternDetails {
        private final AEItemKey definition;
        private final MysticalInfusionPatternData data;
        private final IInput[] inputs;
        private final GenericStack[] outputs;
        private MysticalPatternDetails(AEItemKey definition, MysticalInfusionPatternData data) {
            this.definition = definition; this.data = data;
            Map<AEItemKey, Long> amounts = new LinkedHashMap<>();
            for (ItemStack stack : data.inputs()) {
                if (stack.isEmpty()) continue;
                AEItemKey key = AEItemKey.of(stack);
                if (key != null) amounts.merge(key, (long) stack.getCount(), Long::sum);
            }
            inputs = amounts.entrySet().stream().map(e -> new ExactInput(e.getKey(), e.getValue())).toArray(IInput[]::new);
            AEItemKey output = AEItemKey.of(data.output());
            outputs = output == null ? new GenericStack[0] : new GenericStack[]{new GenericStack(output, data.output().getCount())};
        }
        @Override public AEItemKey getDefinition() { return definition; }
        @Override public IInput[] getInputs() { return inputs; }
        @Override public GenericStack[] getOutputs() { return outputs; }
        @Override public boolean equals(Object other) { return this == other || other instanceof MysticalPatternDetails d && definition.equals(d.definition); }
        @Override public int hashCode() { return definition.hashCode(); }
    }

    private record ExactInput(AEItemKey key, long multiplier) implements IPatternDetails.IInput {
        @Override public GenericStack[] getPossibleInputs() { return new GenericStack[]{new GenericStack(key, 1)}; }
        @Override public long getMultiplier() { return multiplier; }
        @Override public boolean isValid(AEKey candidate, Level level) { return key.equals(candidate); }
        @Override public @Nullable AEKey getRemainingKey(AEKey template) { return null; }
    }
}
