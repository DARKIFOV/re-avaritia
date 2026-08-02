package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.pattern.ExtremePatternData;
import ru.rfvv.metatechreborn.recipe.MachineRecipeMatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** Persistent, server-validated parallel job storage for the extreme assembler. */
final class MolecularAssemblerJobQueue {
    static final int MAX_JOBS = 64;

    // Official AE2 1.20.1 molecular-assembler progression for 0..5 speed cards.
    private static final int[] WORK_PER_TICK = {10, 13, 17, 20, 25, 50};
    private static final int[] POWER_PERCENT = {100, 130, 170, 200, 250, 500};

    private final MolecularAssemblerBlockEntity host;
    private final List<Job> jobs = new ArrayList<>();

    MolecularAssemblerJobQueue(MolecularAssemblerBlockEntity host) {
        this.host = host;
    }

    boolean canAccept() { return jobs.size() < MAX_JOBS; }
    boolean isEmpty() { return jobs.isEmpty(); }
    boolean isFull() { return jobs.size() >= MAX_JOBS; }
    int size() { return jobs.size(); }

    boolean enqueue(ExtremePatternData pattern, NonNullList<ItemStack> placement) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide || !canAccept()
                || placement.size() != MolecularAssemblerBlockEntity.GRID_SLOTS
                || pattern.output().isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < MolecularAssemblerBlockEntity.GRID_SLOTS; slot++) {
            ItemStack expected = pattern.inputs().get(slot);
            ItemStack supplied = placement.get(slot);
            if (expected.isEmpty() != supplied.isEmpty()) return false;
            if (!expected.isEmpty()
                    && (!ItemStack.isSameItemSameTags(expected, supplied) || supplied.getCount() != 1)) {
                return false;
            }
        }

        GridView grid = new GridView(placement);
        Optional<MachineRecipeMatch> resolved = host.findAnyMatch(level, grid);
        if (resolved.isEmpty() || !sameStackAndCount(resolved.get().result(), pattern.output())) return false;

        MachineRecipeMatch match = resolved.get();
        jobs.add(new Job(copyGrid(placement), pattern.output().copy(), match.id(), match.source(), 0));
        host.markChangedAndRunning();
        return true;
    }

    TickState tick(Level level) {
        if (jobs.isEmpty()) return TickState.IDLE;

        boolean progressed = false;
        boolean lackedEnergy = false;
        boolean outputBlocked = false;
        boolean returnedInvalid = false;
        int speedCards = Math.min(5, host.getSpeedCardCount());
        int workPerTick = WORK_PER_TICK[speedCards];

        Iterator<Job> iterator = jobs.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            GridView grid = new GridView(job.inputs);
            Optional<MachineRecipeMatch> resolved = host.findMatch(
                    level, job.recipeId, job.recipeSource, grid);

            if (resolved.isEmpty() || !sameStackAndCount(resolved.get().result(), job.expectedOutput)) {
                if (host.queueStacksForNetwork(job.inputs)) {
                    iterator.remove();
                    returnedInvalid = true;
                    MetaTechReborn.LOGGER.warn(
                            "Refunded invalid queued molecular-assembler job at {} for {}",
                            host.getBlockPos(), job.expectedOutput.getHoverName().getString());
                } else {
                    outputBlocked = true;
                }
                continue;
            }

            MachineRecipeMatch match = resolved.get();
            int requiredWork = Math.max(1, match.craftTime()) * 10;

            // A finished job waiting for return-buffer space must not consume FE again.
            if (job.work >= requiredWork) {
                if (finishJob(job, match)) iterator.remove();
                else outputBlocked = true;
                continue;
            }

            int energyCost = scaledPower(match.energyPerTick(), speedCards);
            if (!host.consumeAssemblerEnergy(energyCost)) {
                lackedEnergy = true;
                continue;
            }

            job.work = Math.min(requiredWork, job.work + workPerTick);
            progressed = true;
            if (job.work >= requiredWork) {
                if (finishJob(job, match)) iterator.remove();
                else outputBlocked = true;
            }
        }

        if (progressed || returnedInvalid) host.setChanged();
        if (outputBlocked) return TickState.OUTPUT_BLOCKED;
        if (progressed) return TickState.RUNNING;
        if (lackedEnergy) return TickState.NO_ENERGY;
        return TickState.WAITING;
    }

    private boolean finishJob(Job job, MachineRecipeMatch match) {
        List<ItemStack> products = new ArrayList<>();
        for (ItemStack remainder : match.remainingItems()) {
            if (!remainder.isEmpty()) products.add(remainder.copy());
        }
        products.add(match.result().copy());
        return host.queueStacksForNetwork(products);
    }

    private static int scaledPower(int base, int speedCards) {
        if (base <= 0) return 0;
        long scaled = (long) base * POWER_PERCENT[Math.min(5, speedCards)];
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (scaled + 99L) / 100L));
    }

    void addDrops(NonNullList<ItemStack> drops) {
        for (Job job : jobs) {
            for (ItemStack stack : job.inputs) {
                if (!stack.isEmpty()) drops.add(stack.copy());
            }
        }
    }

    CompoundTag save() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Job job : jobs) list.add(job.save());
        root.put("Jobs", list);
        root.putInt("Version", 1);
        return root;
    }

    void load(CompoundTag root) {
        jobs.clear();
        ListTag list = root.getList("Jobs", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size() && jobs.size() < MAX_JOBS; index++) {
            Job job = Job.load(list.getCompound(index));
            if (job != null) jobs.add(job);
        }
    }

    private static boolean sameStackAndCount(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second) && first.getCount() == second.getCount();
    }

    private static NonNullList<ItemStack> copyGrid(List<ItemStack> source) {
        NonNullList<ItemStack> copy = NonNullList.withSize(
                MolecularAssemblerBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < copy.size(); slot++) copy.set(slot, source.get(slot).copy());
        return copy;
    }

    enum TickState {
        IDLE,
        WAITING,
        RUNNING,
        NO_ENERGY,
        OUTPUT_BLOCKED
    }

    private static final class Job {
        private final NonNullList<ItemStack> inputs;
        private final ItemStack expectedOutput;
        private final ResourceLocation recipeId;
        private final MachineRecipeMatch.Source recipeSource;
        private int work;

        private Job(NonNullList<ItemStack> inputs, ItemStack expectedOutput,
                    ResourceLocation recipeId, MachineRecipeMatch.Source recipeSource, int work) {
            this.inputs = inputs;
            this.expectedOutput = expectedOutput;
            this.recipeId = recipeId;
            this.recipeSource = recipeSource;
            this.work = Math.max(0, work);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Recipe", recipeId.toString());
            tag.putString("Source", recipeSource.name());
            tag.putInt("Work", work);
            tag.put("Output", expectedOutput.save(new CompoundTag()));

            ListTag inputList = new ListTag();
            for (int slot = 0; slot < inputs.size(); slot++) {
                ItemStack stack = inputs.get(slot);
                if (stack.isEmpty()) continue;
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) slot);
                entry.put("Stack", stack.save(new CompoundTag()));
                inputList.add(entry);
            }
            tag.put("Inputs", inputList);
            return tag;
        }

        private static Job load(CompoundTag tag) {
            ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
            ItemStack output = tag.contains("Output", Tag.TAG_COMPOUND)
                    ? ItemStack.of(tag.getCompound("Output")) : ItemStack.EMPTY;
            if (recipeId == null || output.isEmpty()) return null;

            MachineRecipeMatch.Source source;
            try {
                source = MachineRecipeMatch.Source.valueOf(tag.getString("Source"));
            } catch (IllegalArgumentException error) {
                return null;
            }

            NonNullList<ItemStack> inputs = NonNullList.withSize(
                    MolecularAssemblerBlockEntity.GRID_SLOTS, ItemStack.EMPTY);
            ListTag inputList = tag.getList("Inputs", Tag.TAG_COMPOUND);
            for (int index = 0; index < inputList.size(); index++) {
                CompoundTag entry = inputList.getCompound(index);
                int slot = entry.getByte("Slot") & 255;
                if (slot < inputs.size() && entry.contains("Stack", Tag.TAG_COMPOUND)) {
                    ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
                    if (!stack.isEmpty()) inputs.set(slot, stack);
                }
            }
            return new Job(inputs, output, recipeId, source, tag.getInt("Work"));
        }
    }

    private static final class GridView implements IItemHandler {
        private final List<ItemStack> stacks;

        private GridView(List<ItemStack> stacks) {
            this.stacks = stacks;
        }

        @Override public int getSlots() { return MolecularAssemblerBlockEntity.GRID_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return stacks.get(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    }
}
