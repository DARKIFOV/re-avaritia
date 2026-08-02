package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.block.ManaDrillBlock;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.item.ManaDrillUpgradeItem;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;
import ru.rfvv.metatechreborn.multiblock.ManaDrillStructure;
import ru.rfvv.metatechreborn.recipe.ManaDrillRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.List;
import java.util.Optional;

public final class ManaDrillBlockEntity extends BlockEntity
        implements MenuProvider, ManaReceiver, SparkAttachable {
    public static final int MODULE_SLOT = 0;
    public static final int SPEED_SLOT = 1;
    public static final int LOOTING_SLOT = 2;
    public static final int GENERATION_SLOT = 3;
    public static final int FIRST_OUTPUT_SLOT = 4;
    public static final int OUTPUT_SLOTS = 27;
    public static final int TOTAL_SLOTS = FIRST_OUTPUT_SLOT + OUTPUT_SLOTS;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_STRUCTURE_MISSING = 1;
    public static final int STATUS_NO_MODULE = 2;
    public static final int STATUS_NO_RECIPE = 3;
    public static final int STATUS_NO_MANA = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_RUNNING = 6;

    private int mana;
    private int progress;
    private int maxProgress;
    private int status = STATUS_IDLE;
    private boolean structureFormed;
    private long nextStructureCheck;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot <= GENERATION_SLOT) {
                progress = 0;
                maxProgress = 0;
            }
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot <= GENERATION_SLOT ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case MODULE_SLOT -> stack.is(ModItems.MANA_DRILL_MODULE.get());
                case SPEED_SLOT -> isUpgrade(stack, ManaDrillUpgradeItem.Type.SPEED);
                case LOOTING_SLOT -> isUpgrade(stack, ManaDrillUpgradeItem.Type.LOOTING);
                case GENERATION_SLOT -> isUpgrade(stack, ManaDrillUpgradeItem.Type.GENERATION);
                default -> slot >= FIRST_OUTPUT_SLOT && slot < TOTAL_SLOTS;
            };
        }
    };

    private final IItemHandler externalItems = new IItemHandler() {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot < FIRST_OUTPUT_SLOT ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= FIRST_OUTPUT_SLOT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> externalItems);

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> mana;
                case 3 -> getManaCapacity();
                case 4 -> getUpgradeLevel(SPEED_SLOT, ManaDrillUpgradeItem.Type.SPEED,
                        CommonConfig.MANA_DRILL_MAX_SPEED_UPGRADES.get());
                case 5 -> getUpgradeLevel(LOOTING_SLOT, ManaDrillUpgradeItem.Type.LOOTING,
                        CommonConfig.MANA_DRILL_MAX_LOOTING_UPGRADES.get());
                case 6 -> getUpgradeLevel(GENERATION_SLOT, ManaDrillUpgradeItem.Type.GENERATION,
                        CommonConfig.MANA_DRILL_MAX_GENERATION_UPGRADES.get());
                case 7 -> structureFormed ? 1 : 0;
                case 8 -> status;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 2) mana = value;
            else if (index == 8) status = value;
        }
        @Override public int getCount() { return 9; }
    };

    public ManaDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_DRILL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ManaDrillBlockEntity blockEntity) {
        blockEntity.tickServer(level);
    }

    private void tickServer(Level level) {
        if (level.getGameTime() >= nextStructureCheck) {
            updateStructureState(level);
            nextStructureCheck = level.getGameTime() + 20L;
        }

        if (level.getGameTime() % CommonConfig.MANA_DRILL_POOL_SCAN_INTERVAL.get() == 0L) {
            pullManaFromNearbyPools(level);
        }

        if (!structureFormed) {
            resetProgress();
            setStatus(STATUS_STRUCTURE_MISSING);
            autoEjectIfNeeded(level);
            return;
        }

        ItemStack module = items.getStackInSlot(MODULE_SLOT);
        if (module.isEmpty()) {
            resetProgress();
            setStatus(STATUS_NO_MODULE);
            autoEjectIfNeeded(level);
            return;
        }

        Optional<ManaDrillRecipe> match = findRecipe(level);
        if (match.isEmpty()) {
            resetProgress();
            setStatus(STATUS_NO_RECIPE);
            autoEjectIfNeeded(level);
            return;
        }

        if (!hasAnyOutputSpace()) {
            setStatus(STATUS_OUTPUT_FULL);
            autoEjectIfNeeded(level);
            return;
        }

        ManaDrillRecipe recipe = match.get();
        int speedLevel = getUpgradeLevel(SPEED_SLOT, ManaDrillUpgradeItem.Type.SPEED,
                CommonConfig.MANA_DRILL_MAX_SPEED_UPGRADES.get());
        int lootingLevel = getUpgradeLevel(LOOTING_SLOT, ManaDrillUpgradeItem.Type.LOOTING,
                CommonConfig.MANA_DRILL_MAX_LOOTING_UPGRADES.get());
        int generationLevel = getUpgradeLevel(GENERATION_SLOT, ManaDrillUpgradeItem.Type.GENERATION,
                CommonConfig.MANA_DRILL_MAX_GENERATION_UPGRADES.get());

        maxProgress = Math.max(1, recipe.time() / (1 + speedLevel));
        if (mana < recipe.manaCost()) {
            setStatus(STATUS_NO_MANA);
            autoEjectIfNeeded(level);
            return;
        }

        setStatus(STATUS_RUNNING);
        progress++;
        setChanged();
        if (progress >= maxProgress) {
            List<ItemStack> generated = recipe.rollDrops(level.random, lootingLevel, generationLevel);
            if (generated.isEmpty() || canInsertAll(generated)) {
                receiveMana(-recipe.manaCost());
                for (ItemStack stack : generated) insertOutput(stack, false);
                progress = 0;
                setChanged();
            } else {
                progress = maxProgress;
                setStatus(STATUS_OUTPUT_FULL);
            }
        }
        autoEjectIfNeeded(level);
    }

    private void updateStructureState(Level level) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(ManaDrillBlock.FACING)
                ? state.getValue(ManaDrillBlock.FACING) : Direction.NORTH;
        boolean formed = ManaDrillStructure.isFormed(level, worldPosition, facing);
        if (formed != structureFormed) {
            structureFormed = formed;
            if (!formed) resetProgress();
            setChanged();
        }
    }

    public void forceStructureCheck() {
        nextStructureCheck = 0L;
        if (level != null && !level.isClientSide) updateStructureState(level);
    }

    public boolean isStructureFormed() { return structureFormed; }

    private static boolean isUpgrade(ItemStack stack, ManaDrillUpgradeItem.Type expectedType) {
        return stack.getItem() instanceof ManaDrillUpgradeItem upgrade && upgrade.type() == expectedType;
    }

    private int getUpgradeLevel(int slot, ManaDrillUpgradeItem.Type expectedType, int configuredMaximum) {
        ItemStack stack = items.getStackInSlot(slot);
        if (!(stack.getItem() instanceof ManaDrillUpgradeItem upgrade) || upgrade.type() != expectedType) return 0;
        return Math.min(Math.max(0, configuredMaximum), upgrade.level());
    }

    private Optional<ManaDrillRecipe> findRecipe(Level level) {
        ItemStack module = items.getStackInSlot(MODULE_SLOT);
        if (module.isEmpty()) return Optional.empty();
        Optional<ManaDrillRecipe> dataRecipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.MANA_DRILL_GENERATING_TYPE.get())
                .stream().filter(recipe -> recipe.matchesModule(module)).findFirst();
        if (dataRecipe.isPresent()) return dataRecipe;
        if (!module.is(ModItems.MANA_DRILL_MODULE.get())) return Optional.empty();
        return Optional.of(createFallbackRecipe());
    }

    private static ManaDrillRecipe createFallbackRecipe() {
        return new ManaDrillRecipe(
                new ResourceLocation(MetaTechReborn.MOD_ID, "runtime_mana_drill/default"),
                Ingredient.of(ModItems.MANA_DRILL_MODULE.get()),
                25_000,
                240,
                List.of(
                        new ManaDrillRecipe.Drop(new ItemStack(Items.COBBLESTONE), 4, 12, 10_000),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.COAL), 1, 4, 6_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.RAW_IRON), 1, 3, 5_000),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.RAW_COPPER), 1, 4, 5_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.RAW_GOLD), 1, 2, 2_800),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.REDSTONE), 2, 6, 3_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.LAPIS_LAZULI), 2, 6, 2_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.DIAMOND), 1, 2, 650),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.EMERALD), 1, 2, 400),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.ANCIENT_DEBRIS), 1, 1, 80)
                )
        );
    }

    private void setStatus(int newStatus) {
        if (status != newStatus) {
            status = newStatus;
            setChanged();
        }
    }

    private void resetProgress() {
        if (progress != 0 || maxProgress != 0) {
            progress = 0;
            maxProgress = 0;
            setChanged();
        }
    }

    private void pullManaFromNearbyPools(Level level) {
        if (isFull()) return;
        int radius = CommonConfig.MANA_DRILL_POOL_SCAN_RADIUS.get();
        int remainingTransfer = CommonConfig.MANA_DRILL_MAX_POOL_TRANSFER.get();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius && remainingTransfer > 0 && !isFull(); x++) {
            for (int y = -radius; y <= radius && remainingTransfer > 0 && !isFull(); y++) {
                for (int z = -radius; z <= radius && remainingTransfer > 0 && !isFull(); z++) {
                    cursor.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                    BlockEntity candidate = level.getBlockEntity(cursor);
                    if (!(candidate instanceof ManaPool pool) || candidate == this) continue;
                    int amount = Math.min(remainingTransfer,
                            Math.min(pool.getCurrentMana(), getManaCapacity() - mana));
                    if (amount <= 0) continue;
                    pool.receiveMana(-amount);
                    receiveMana(amount);
                    remainingTransfer -= amount;
                }
            }
        }
    }

    private boolean hasAnyOutputSpace() {
        for (int slot = FIRST_OUTPUT_SLOT; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    private boolean canInsertAll(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(OUTPUT_SLOTS);
        for (int index = 0; index < OUTPUT_SLOTS; index++) {
            copy.setStackInSlot(index, items.getStackInSlot(FIRST_OUTPUT_SLOT + index).copy());
        }
        for (ItemStack source : stacks) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(copy, source.copy(), false);
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }

    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = FIRST_OUTPUT_SLOT; slot < TOTAL_SLOTS && !remainder.isEmpty(); slot++) {
            remainder = items.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private void autoEjectIfNeeded(Level level) {
        if (CommonConfig.MANA_DRILL_AUTO_EJECT.get() && level.getGameTime() % 5L == 0L) autoEject(level);
    }

    private void autoEject(Level level) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) continue;
            Optional<IItemHandler> target = neighbour.getCapability(
                    ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve();
            if (target.isEmpty()) continue;
            for (int slot = FIRST_OUTPUT_SLOT; slot < TOTAL_SLOTS; slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target.get(), stack.copy(), false);
                if (remainder.getCount() != stack.getCount()) items.setStackInSlot(slot, remainder);
            }
        }
    }

    public ItemStackHandler getItems() { return items; }
    public ContainerData getData() { return data; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.mana_drill");
    }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                                 @NotNull Player player) {
        return new ManaDrillMenu(id, inventory, this, data);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Mana", mana);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Status", status);
        tag.putBoolean("StructureFormed", structureFormed);
    }

    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        mana = Math.min(getManaCapacity(), Math.max(0, tag.getInt("Mana")));
        progress = Math.max(0, tag.getInt("Progress"));
        maxProgress = Math.max(0, tag.getInt("MaxProgress"));
        status = tag.getInt("Status");
        structureFormed = tag.getBoolean("StructureFormed");
        nextStructureCheck = 0L;
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override public Level getManaReceiverLevel() { return level; }
    @Override public BlockPos getManaReceiverPos() { return worldPosition; }
    @Override public int getCurrentMana() { return mana; }
    @Override public boolean isFull() { return mana >= getManaCapacity(); }

    @Override
    public void receiveMana(int amount) {
        int updated = Math.max(0, Math.min(getManaCapacity(), mana + amount));
        if (updated != mana) {
            mana = updated;
            setChanged();
        }
    }

    @Override public boolean canReceiveManaFromBursts() { return !isFull(); }
    @Override public boolean canAttachSpark(ItemStack stack) { return true; }
    @Override public int getAvailableSpaceForMana() { return Math.max(0, getManaCapacity() - mana); }
    @Override public boolean areIncomingTranfersDone() { return isFull(); }

    @Override
    public ManaSpark getAttachedSpark() {
        if (level == null) return null;
        AABB area = new AABB(worldPosition.getX(), worldPosition.getY() + 1.0D, worldPosition.getZ(),
                worldPosition.getX() + 1.0D, worldPosition.getY() + 2.0D, worldPosition.getZ() + 1.0D);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, area,
                entity -> entity instanceof ManaSpark);
        for (Entity entity : entities) {
            if (entity instanceof ManaSpark spark) return spark;
        }
        return null;
    }

    private int getManaCapacity() {
        return CommonConfig.MANA_DRILL_MANA_CAPACITY.get();
    }
}
