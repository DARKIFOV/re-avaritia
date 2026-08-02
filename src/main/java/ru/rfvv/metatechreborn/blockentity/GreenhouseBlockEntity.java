package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
import ru.rfvv.metatechreborn.MetaTechReborn;
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

/** Compact Botania generating-flower greenhouse with a real mana pool interface. */
public final class GreenhouseBlockEntity extends BlockEntity implements MenuProvider, ManaPool, SparkAttachable {
    public static final int FLOWER_SLOT = 0;
    public static final int FIRST_MODULE_SLOT = 1;
    public static final int MODULE_SLOTS = 3;
    public static final int FIRST_FUEL_SLOT = 4;
    public static final int FUEL_SLOTS = 6;
    public static final int TOTAL_SLOTS = 10;
    public static final int MANA_CAPACITY = 2_000_000;
    public static final int FLUID_CAPACITY = 8_000;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_FLOWER = 1;
    public static final int STATUS_UNSUPPORTED_FLOWER = 2;
    public static final int STATUS_NO_FUEL = 3;
    public static final int STATUS_NO_FLUID = 4;
    public static final int STATUS_WRONG_TIME = 5;
    public static final int STATUS_MANA_FULL = 6;
    public static final int STATUS_RUNNING = 7;

    private int mana;
    private int progress;
    private int maxProgress;
    private int economyCycle;
    private int status = STATUS_IDLE;
    private ResourceLocation activeRecipeId;
    private boolean networkRegistered;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot <= FIRST_FUEL_SLOT + FUEL_SLOTS - 1) resetProgress(false);
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == FLOWER_SLOT) return 1;
            if (slot >= FIRST_MODULE_SLOT && slot < FIRST_MODULE_SLOT + MODULE_SLOTS) return 1;
            return super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FLOWER_SLOT) return isSupportedFlowerItem(stack);
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
            resetProgress(false);
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
                case 10 -> status;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 2) mana = value;
            else if (index == 10) status = value;
        }

        @Override public int getCount() { return 11; }
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

        ItemStack flower = items.getStackInSlot(FLOWER_SLOT);
        if (flower.isEmpty()) {
            activeRecipeId = null;
            resetProgress(false);
            setStatus(STATUS_NO_FLOWER);
            return;
        }

        Optional<GreenhouseRecipe> candidate = findRecipeByFlower(level, flower);
        if (candidate.isEmpty()) {
            activeRecipeId = null;
            resetProgress(false);
            setStatus(STATUS_UNSUPPORTED_FLOWER);
            return;
        }

        GreenhouseRecipe recipe = candidate.get();
        if (!recipe.getId().equals(activeRecipeId)) {
            activeRecipeId = recipe.getId();
            progress = 0;
            maxProgress = 0;
            setChanged();
        }

        if (!timeRequirementMet(level, recipe)) {
            resetProgress(false);
            setStatus(STATUS_WRONG_TIME);
            return;
        }
        if (recipe.requiresFuel() && findFuelSlot(recipe) < 0) {
            resetProgress(false);
            setStatus(STATUS_NO_FUEL);
            return;
        }
        if (!hasRequiredFluid(recipe)) {
            resetProgress(false);
            setStatus(STATUS_NO_FLUID);
            return;
        }

        int generatedMana = getGeneratedMana(recipe);
        if (mana > MANA_CAPACITY - generatedMana) {
            resetProgress(false);
            setStatus(STATUS_MANA_FULL);
            return;
        }

        int speed = getModuleLevel(GreenhouseModuleItem.Type.SPEED);
        maxProgress = Math.max(1, recipe.time() * Math.max(25, 100 - speed * 20) / 100);
        setStatus(STATUS_RUNNING);
        progress++;
        setChanged();

        if (progress < maxProgress) return;
        if (!consumeInputs(recipe)) {
            resetProgress(false);
            setStatus(recipe.requiresFluid() && !hasRequiredFluid(recipe) ? STATUS_NO_FLUID : STATUS_NO_FUEL);
            return;
        }

        receiveMana(generatedMana);
        progress = 0;
        setChanged();
    }

    private Optional<GreenhouseRecipe> findRecipeByFlower(Level level, ItemStack flower) {
        for (GreenhouseRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.GREENHOUSE_TYPE.get())) {
            if (recipe.matchesFlower(flower)) return Optional.of(recipe);
        }
        return createFallbackRecipe(flower);
    }

    private Optional<GreenhouseRecipe> createFallbackRecipe(ItemStack flower) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(flower.getItem());
        if (!"botania".equals(id.getNamespace())) return Optional.empty();

        Ingredient flowerIngredient = Ingredient.of(flower.getItem());
        String path = id.getPath();
        GreenhouseRecipe recipe = switch (path) {
            case "endoflame" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.of(ItemTags.COALS), Fluids.EMPTY, 0, 1_200, 200, true, false, false);
            case "hydroangeas" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.EMPTY, Fluids.WATER, 250, 400, 200, false, false, false);
            case "gourmaryllis" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.of(Items.BREAD), Fluids.EMPTY, 0, 5_000, 200, true, false, false);
            case "entropinnyum" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.of(Items.TNT), Fluids.EMPTY, 0, 6_500, 200, true, false, false);
            case "thermalily" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.EMPTY, Fluids.LAVA, 1_000, 18_000, 400, false, false, false);
            case "spectrolus" -> new GreenhouseRecipe(runtimeId(path), flowerIngredient,
                    Ingredient.of(ItemTags.WOOL), Fluids.EMPTY, 0, 1_200, 100, true, false, false);
            default -> null;
        };
        return Optional.ofNullable(recipe);
    }

    private static ResourceLocation runtimeId(String flowerPath) {
        return new ResourceLocation(MetaTechReborn.MOD_ID, "runtime_greenhouse/" + flowerPath);
    }

    private boolean timeRequirementMet(Level level, GreenhouseRecipe recipe) {
        boolean infiniteDay = hasModule(GreenhouseModuleItem.Type.INFINITE_DAY);
        boolean infiniteNight = hasModule(GreenhouseModuleItem.Type.INFINITE_NIGHT);
        boolean effectiveDay = infiniteDay || (!infiniteNight && level.isDay());
        boolean effectiveNight = infiniteNight || (!infiniteDay && !level.isDay());
        return (!recipe.dayOnly() || effectiveDay) && (!recipe.nightOnly() || effectiveNight);
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
            int required = getAdjustedFluidCost(recipe);
            int drained = tank.drain(required, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < required) return false;
        }

        if (recipe.requiresFuel() && recipe.consumeFuel()) {
            int fuelSlot = findFuelSlot(recipe);
            if (fuelSlot < 0) return false;
            int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
            economyCycle++;
            if (economyCycle >= 1 + economy) {
                ItemStack fuel = items.getStackInSlot(fuelSlot).copy();
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
        return Math.min(type.maximum(), level);
    }

    private boolean hasModule(GreenhouseModuleItem.Type type) {
        return getModuleLevel(type) > 0;
    }

    private static boolean isSupportedFlowerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"botania".equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "endoflame", "hydroangeas", "gourmaryllis", "entropinnyum", "thermalily", "spectrolus" -> true;
            default -> false;
        };
    }

    private int getModeId() {
        if (activeRecipeId == null) return 0;
        String path = activeRecipeId.getPath();
        if (path.endsWith("endoflame")) return 1;
        if (path.endsWith("hydroangeas")) return 2;
        if (path.endsWith("gourmaryllis")) return 3;
        if (path.endsWith("entropinnyum")) return 4;
        if (path.endsWith("thermalily")) return 5;
        if (path.endsWith("spectrolus")) return 6;
        return 7;
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

    private void setStatus(int newStatus) {
        if (status != newStatus) {
            status = newStatus;
            setChanged();
        }
    }

    private void resetProgress(boolean clearRecipe) {
        boolean changed = progress != 0 || maxProgress != 0 || (clearRecipe && activeRecipeId != null);
        progress = 0;
        maxProgress = 0;
        if (clearRecipe) activeRecipeId = null;
        if (changed) setChanged();
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

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.metatech_reborn.greenhouse");
    }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory,
                                                                 @NotNull Player player) {
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
        tag.putInt("Status", status);
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
        status = tag.getInt("Status");
        activeRecipeId = tag.contains("ActiveRecipe")
                ? ResourceLocation.tryParse(tag.getString("ActiveRecipe")) : null;
        networkRegistered = false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
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
