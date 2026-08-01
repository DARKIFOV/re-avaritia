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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rfvv.metatechreborn.item.GreenhouseModuleItem;
import ru.rfvv.metatechreborn.menu.GreenhouseMenu;
import ru.rfvv.metatechreborn.recipe.GreenhouseRecipe;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModRecipes;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.ManaBlockType;
import vazkii.botania.api.mana.ManaNetworkAction;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.List;
import java.util.Optional;

/**
 * Server-side greenhouse that emulates Botania generating flowers in a compact machine.
 * It stores generated mana as a real Botania ManaPool and can export to nearby pools or Sparks.
 */
public final class GreenhouseBlockEntity extends BlockEntity implements MenuProvider, ManaPool, SparkAttachable {
    public static final int FLOWER_SLOT = 0;
    public static final int FIRST_MODULE_SLOT = 1;
    public static final int MODULE_SLOTS = 3;
    public static final int FIRST_FUEL_SLOT = 4;
    public static final int FUEL_SLOTS = 6;
    public static final int TOTAL_SLOTS = 10;
    public static final int MANA_CAPACITY = 2_000_000;
    public static final int FLUID_CAPACITY = 8_000;

    private int mana;
    private int progress;
    private int maxProgress;
    private int economyCycle;
    private ResourceLocation activeRecipeId;
    private boolean networkRegistered;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot <= FIRST_FUEL_SLOT + FUEL_SLOTS - 1) resetProgress();
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot >= FIRST_MODULE_SLOT && slot < FIRST_MODULE_SLOT + MODULE_SLOTS ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FLOWER_SLOT) return true;
            if (slot >= FIRST_MODULE_SLOT && slot < FIRST_MODULE_SLOT + MODULE_SLOTS) {
                return stack.getItem() instanceof GreenhouseModuleItem;
            }
            return slot >= FIRST_FUEL_SLOT && slot < FIRST_FUEL_SLOT + FUEL_SLOTS;
        }
    };

    private final FluidTank tank = new FluidTank(FLUID_CAPACITY,
            stack -> stack.getFluid() == Fluids.WATER || stack.getFluid() == Fluids.LAVA) {
        @Override
        protected void onContentsChanged() {
            resetProgress();
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> mana;
                case 3 -> MANA_CAPACITY;
                case 4 -> tank.getFluidAmount();
                case 5 -> tank.getCapacity();
                case 6 -> getModuleLevel(GreenhouseModuleItem.Type.SPEED);
                case 7 -> getModuleLevel(GreenhouseModuleItem.Type.EFFICIENCY);
                case 8 -> getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
                case 9 -> getModeId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 2) mana = value;
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GREENHOUSE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GreenhouseBlockEntity greenhouse) {
        greenhouse.tickServer(level);
    }

    private void tickServer(Level level) {
        ensureManaNetworkRegistration();
        exportManaToNearbyPools(level);

        Optional<GreenhouseRecipe> match = findRecipe(level);
        if (match.isEmpty()) {
            resetProgress();
            return;
        }

        GreenhouseRecipe recipe = match.get();
        if (!recipe.getId().equals(activeRecipeId)) {
            activeRecipeId = recipe.getId();
            progress = 0;
        }

        int generatedMana = getGeneratedMana(recipe);
        if (mana > MANA_CAPACITY - generatedMana) return;

        int speed = getModuleLevel(GreenhouseModuleItem.Type.SPEED);
        maxProgress = Math.max(1, recipe.time() * Math.max(25, 100 - speed * 20) / 100);
        progress++;
        setChanged();

        if (progress < maxProgress) return;
        if (!consumeInputs(recipe)) {
            resetProgress();
            return;
        }

        receiveMana(generatedMana);
        progress = 0;
        setChanged();
    }

    private Optional<GreenhouseRecipe> findRecipe(Level level) {
        ItemStack flower = items.getStackInSlot(FLOWER_SLOT);
        if (flower.isEmpty()) return Optional.empty();

        boolean effectiveDay = hasModule(GreenhouseModuleItem.Type.INFINITE_DAY)
                || (!hasModule(GreenhouseModuleItem.Type.INFINITE_NIGHT) && level.isDay());
        boolean effectiveNight = hasModule(GreenhouseModuleItem.Type.INFINITE_NIGHT)
                || (!hasModule(GreenhouseModuleItem.Type.INFINITE_DAY) && !level.isDay());

        for (GreenhouseRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.GREENHOUSE_TYPE.get())) {
            if (!recipe.matchesFlower(flower)) continue;
            if (recipe.dayOnly() && !effectiveDay) continue;
            if (recipe.nightOnly() && !effectiveNight) continue;
            if (recipe.requiresFuel() && findFuelSlot(recipe) < 0) continue;
            if (!hasRequiredFluid(recipe)) continue;
            return Optional.of(recipe);
        }
        return Optional.empty();
    }

    private int findFuelSlot(GreenhouseRecipe recipe) {
        for (int slot = FIRST_FUEL_SLOT; slot < FIRST_FUEL_SLOT + FUEL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty() && recipe.matchesFuel(stack)) return slot;
        }
        return -1;
    }

    private boolean hasRequiredFluid(GreenhouseRecipe recipe) {
        if (!recipe.requiresFluid()) return true;
        if (recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA)) return true;
        int required = getAdjustedFluidCost(recipe);
        FluidStack stored = tank.getFluid();
        return !stored.isEmpty() && stored.getFluid() == recipe.fluid() && stored.getAmount() >= required;
    }

    private boolean consumeInputs(GreenhouseRecipe recipe) {
        if (recipe.requiresFluid()
                && !(recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA))) {
            int drained = tank.drain(getAdjustedFluidCost(recipe), IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < getAdjustedFluidCost(recipe)) return false;
        }

        if (recipe.requiresFuel() && recipe.consumeFuel()) {
            int fuelSlot = findFuelSlot(recipe);
            if (fuelSlot < 0) return false;
            int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
            economyCycle++;
            if (economyCycle >= 1 + economy) {
                ItemStack fuel = items.getStackInSlot(fuelSlot);
                fuel.shrink(1);
                items.setStackInSlot(fuelSlot, fuel);
                economyCycle = 0;
            }
        }
        return true;
    }

    private int getAdjustedFluidCost(GreenhouseRecipe recipe) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        return Math.max(1, recipe.fluidAmount() * Math.max(40, 100 - economy * 20) / 100);
    }

    private int getGeneratedMana(GreenhouseRecipe recipe) {
        int efficiency = getModuleLevel(GreenhouseModuleItem.Type.EFFICIENCY);
        long value = (long) recipe.mana() * (100 + efficiency * 25) / 100;
        return (int) Math.min(MANA_CAPACITY, Math.max(1L, value));
    }

    private int getModuleLevel(GreenhouseModuleItem.Type type) {
        int level = 0;
        for (int slot = FIRST_MODULE_SLOT; slot < FIRST_MODULE_SLOT + MODULE_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.getItem() instanceof GreenhouseModuleItem module && module.type() == type) {
                level += module.level();
            }
        }
        return Math.min(3, level);
    }

    private boolean hasModule(GreenhouseModuleItem.Type type) {
        return getModuleLevel(type) > 0;
    }

    private int getModeId() {
        if (activeRecipeId == null) return 0;
        return switch (activeRecipeId.getPath()) {
            case "endoflame" -> 1;
            case "hydroangeas" -> 2;
            case "gourmaryllis" -> 3;
            case "entropinnyum" -> 4;
            case "thermalily" -> 5;
            case "spectrolus" -> 6;
            default -> 7;
        };
    }

    private void exportManaToNearbyPools(Level level) {
        if (mana <= 0 || level.getGameTime() % 2L != 0L) return;
        int remaining = Math.min(10_000, mana);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -2; x <= 2 && remaining > 0; x++) {
            for (int y = -2; y <= 2 && remaining > 0; y++) {
                for (int z = -2; z <= 2 && remaining > 0; z++) {
                    cursor.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                    BlockEntity candidate = level.getBlockEntity(cursor);
                    if (!(candidate instanceof ManaPool pool) || candidate == this || pool.isOutputtingPower()) continue;
                    int space = Math.max(0, pool.getMaxMana() - pool.getCurrentMana());
                    int amount = Math.min(remaining, space);
                    if (amount <= 0) continue;
                    pool.receiveMana(amount);
                    receiveMana(-amount);
                    remaining -= amount;
                }
            }
        }
    }

    private void ensureManaNetworkRegistration() {
        if (!networkRegistered && level != null && !isRemoved()) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(
                    this, ManaBlockType.POOL, ManaNetworkAction.ADD);
            networkRegistered = true;
        }
    }

    private void resetProgress() {
        if (progress != 0 || maxProgress != 0 || activeRecipeId != null) {
            progress = 0;
            maxProgress = 0;
            activeRecipeId = null;
            setChanged();
        }
    }

    public ItemStackHandler getItems() { return items; }
    public FluidTank getTank() { return tank; }
    public ContainerData getData() { return data; }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
        }
        return drops;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.greenhouse");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new GreenhouseMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("Mana", mana);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("EconomyCycle", economyCycle);
        if (activeRecipeId != null) tag.putString("ActiveRecipe", activeRecipeId.toString());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));
        tank.readFromNBT(tag.getCompound("Tank"));
        mana = Math.max(0, Math.min(MANA_CAPACITY, tag.getInt("Mana")));
        progress = Math.max(0, tag.getInt("Progress"));
        maxProgress = Math.max(0, tag.getInt("MaxProgress"));
        economyCycle = Math.max(0, tag.getInt("EconomyCycle"));
        activeRecipeId = tag.contains("ActiveRecipe") ? new ResourceLocation(tag.getString("ActiveRecipe")) : null;
        networkRegistered = false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        fluidCapability.invalidate();
    }

    @Override
    public void setRemoved() {
        if (networkRegistered && level != null) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(
                    this, ManaBlockType.POOL, ManaNetworkAction.REMOVE);
            networkRegistered = false;
        }
        super.setRemoved();
    }

    @Override public Level getManaReceiverLevel() { return level; }
    @Override public BlockPos getManaReceiverPos() { return worldPosition; }
    @Override public int getCurrentMana() { return mana; }
    @Override public boolean isFull() { return mana >= MANA_CAPACITY; }
    @Override public boolean canReceiveManaFromBursts() { return false; }
    @Override public boolean isOutputtingPower() { return true; }
    @Override public int getMaxMana() { return MANA_CAPACITY; }
    @Override public Optional<DyeColor> getColor() { return Optional.empty(); }
    @Override public void setColor(Optional<DyeColor> color) {}

    @Override
    public void receiveMana(int amount) {
        int updated = Math.max(0, Math.min(MANA_CAPACITY, mana + amount));
        if (updated != mana) {
            mana = updated;
            setChanged();
        }
    }

    @Override public boolean canAttachSpark(ItemStack stack) { return true; }
    @Override public int getAvailableSpaceForMana() { return 0; }
    @Override public boolean areIncomingTranfersDone() { return true; }

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
}
