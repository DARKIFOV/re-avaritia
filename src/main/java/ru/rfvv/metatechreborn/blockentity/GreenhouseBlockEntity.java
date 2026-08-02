package ru.rfvv.metatechreborn.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
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
import vazkii.botania.xplat.XplatAbstractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Compact Botania generating-flower greenhouse.
 *
 * <p>Standard flowers use item/fluid equivalents of their original mechanics.
 * Datapacks can add more flowers through the greenhouse recipe type.</p>
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

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_FLOWER = 1;
    public static final int STATUS_UNSUPPORTED_FLOWER = 2;
    public static final int STATUS_NO_FUEL = 3;
    public static final int STATUS_NO_FLUID = 4;
    public static final int STATUS_WRONG_TIME = 5;
    public static final int STATUS_MANA_FULL = 6;
    public static final int STATUS_RUNNING = 7;

    private static final int ENDOFLAME_FUEL_CAP = 32_000;
    private static final int GOURMARYLLIS_MAX_FOOD_VALUE = 12;
    private static final double[] GOURMARYLLIS_STREAK_MULTIPLIERS =
            {0.0D, 1.0D, 1.3D, 1.5D, 1.6D, 1.7D, 1.75D, 1.8D};
    private static final TagKey<Block> BOTANIA_SPECIAL_FLOWERS = TagKey.create(
            Registries.BLOCK, new ResourceLocation("botania", "special_flowers"));
    private static final Set<String> STANDARD_FLOWERS = Set.of(
            "endoflame", "hydroangeas", "gourmaryllis", "entropinnyum", "thermalily",
            "spectrolus", "rosa_arcana", "munchdew", "kekimurus", "narslimmus",
            "shulk_me_not", "dandelifeon", "rafflowsia");

    private int mana;
    private int progress;
    private int maxProgress;
    private int economyCycle;
    private int status = STATUS_IDLE;
    private ResourceLocation activeRecipeId;
    private boolean networkRegistered;

    private int spectrolusNextColor = DyeColor.WHITE.getId();
    private final List<ItemStack> gourmaryllisHistory = new ArrayList<>();
    private int gourmaryllisStreak = -1;
    private int gourmaryllisRepeatCount;
    private final List<ResourceLocation> rafflowsiaHistory = new ArrayList<>();
    private int rafflowsiaStreak = -1;
    private int rafflowsiaRepeatCount;

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
                case 11 -> spectrolusNextColor;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 2) mana = value;
            else if (index == 10) status = value;
            else if (index == 11) spectrolusNextColor = DyeColor.byId(value).getId();
        }

        @Override public int getCount() { return 12; }
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

        int fuelSlot = recipe.requiresFuel() ? findFuelSlot(recipe) : -1;
        if (recipe.requiresFuel() && fuelSlot < 0) {
            resetProgress(false);
            setStatus(STATUS_NO_FUEL);
            return;
        }
        if (!hasRequiredFluid(recipe)) {
            resetProgress(false);
            setStatus(STATUS_NO_FLUID);
            return;
        }

        ItemStack fuel = fuelSlot < 0 ? ItemStack.EMPTY : oneItem(items.getStackInSlot(fuelSlot));
        int generatedMana = getGeneratedMana(recipe, fuel);
        if (mana > MANA_CAPACITY - generatedMana) {
            resetProgress(false);
            setStatus(STATUS_MANA_FULL);
            return;
        }

        int speed = getModuleLevel(GreenhouseModuleItem.Type.SPEED);
        int baseTime = getBaseOperationTime(recipe, fuel);
        maxProgress = Math.max(1, baseTime * Math.max(25, 100 - speed * 20) / 100);
        setStatus(STATUS_RUNNING);
        progress++;
        setChanged();

        if (progress < maxProgress) return;
        if (!consumeInputs(recipe, fuelSlot, fuel)) {
            resetProgress(false);
            setStatus(recipe.requiresFluid() && !hasRequiredFluid(recipe)
                    ? STATUS_NO_FLUID : STATUS_NO_FUEL);
            return;
        }

        afterSuccessfulCycle(recipe, fuel);
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
        String path = normalizeFlowerPath(id.getPath());
        GreenhouseRecipe recipe = switch (path) {
            case "endoflame" -> recipe(path, flowerIngredient, Ingredient.EMPTY,
                    Fluids.EMPTY, 0, 1_200, 200, GreenhouseRecipe.FuelMode.FURNACE_FUEL);
            case "hydroangeas" -> recipe(path, flowerIngredient, Ingredient.EMPTY,
                    Fluids.WATER, 250, 400, 200, GreenhouseRecipe.FuelMode.NONE);
            case "gourmaryllis" -> recipe(path, flowerIngredient, Ingredient.EMPTY,
                    Fluids.EMPTY, 0, 5_000, 200, GreenhouseRecipe.FuelMode.EDIBLE);
            case "entropinnyum" -> recipe(path, flowerIngredient, Ingredient.of(Items.TNT),
                    Fluids.EMPTY, 0, 6_500, 200, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "thermalily" -> recipe(path, flowerIngredient, Ingredient.EMPTY,
                    Fluids.LAVA, 1_000, 18_000, 400, GreenhouseRecipe.FuelMode.NONE);
            case "spectrolus" -> recipe(path, flowerIngredient, Ingredient.of(ItemTags.WOOL),
                    Fluids.EMPTY, 0, 1_200, 100, GreenhouseRecipe.FuelMode.WOOL_CYCLE);
            case "rosa_arcana" -> recipe(path, flowerIngredient, Ingredient.of(Items.EXPERIENCE_BOTTLE),
                    Fluids.EMPTY, 0, 550, 40, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "munchdew" -> recipe(path, flowerIngredient, Ingredient.of(ItemTags.LEAVES),
                    Fluids.EMPTY, 0, 160, 80, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "kekimurus" -> recipe(path, flowerIngredient, Ingredient.of(Items.CAKE),
                    Fluids.EMPTY, 0, 1_800, 80, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "narslimmus" -> recipe(path, flowerIngredient,
                    Ingredient.of(Items.SLIME_BALL, Items.SLIME_BLOCK),
                    Fluids.EMPTY, 0, 1_200, 100, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "shulk_me_not" -> recipe(path, flowerIngredient, Ingredient.of(Items.SHULKER_SHELL),
                    Fluids.EMPTY, 0, 75_000, 1_200, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "dandelifeon" -> recipe(path, flowerIngredient, ingredientFor("botania", "cell_block"),
                    Fluids.EMPTY, 0, 6_000, 200, GreenhouseRecipe.FuelMode.INGREDIENT);
            case "rafflowsia" -> recipe(path, flowerIngredient, Ingredient.EMPTY,
                    Fluids.EMPTY, 0, 2_000, 40, GreenhouseRecipe.FuelMode.SPECIAL_FLOWER);
            default -> null;
        };
        return Optional.ofNullable(recipe);
    }

    private static GreenhouseRecipe recipe(String path, Ingredient flower, Ingredient fuel,
                                           net.minecraft.world.level.material.Fluid fluid,
                                           int fluidAmount, int mana, int time,
                                           GreenhouseRecipe.FuelMode fuelMode) {
        return new GreenhouseRecipe(runtimeId(path), flower, fuel, fluid, fluidAmount, mana, time,
                fuelMode != GreenhouseRecipe.FuelMode.NONE, false, false, fuelMode);
    }

    private static Ingredient ingredientFor(String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(namespace, path));
        return item == Items.AIR ? Ingredient.EMPTY : Ingredient.of(item);
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
            if (!stack.isEmpty() && matchesFuel(recipe, stack)) return slot;
        }
        return -1;
    }

    private boolean matchesFuel(GreenhouseRecipe recipe, ItemStack stack) {
        return switch (recipe.fuelMode()) {
            case NONE -> true;
            case INGREDIENT -> recipe.fuel().test(stack);
            case FURNACE_FUEL -> getBurnTime(stack) > 0
                    && !stack.getItem().hasCraftingRemainingItem();
            case EDIBLE -> getFoodProperties(stack) != null;
            case WOOL_CYCLE -> stack.is(getExpectedWoolItem());
            case SPECIAL_FLOWER -> isConsumableSpecialFlower(stack);
        };
    }

    private boolean isConsumableSpecialFlower(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = normalizeFlowerPath(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if ("rafflowsia".equals(path)) return false;
        Block block = Block.byItem(stack.getItem());
        return block != Blocks.AIR && block.defaultBlockState().is(BOTANIA_SPECIAL_FLOWERS);
    }

    private boolean hasRequiredFluid(GreenhouseRecipe recipe) {
        if (!recipe.requiresFluid()) return true;
        if (recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA)) return true;
        int required = getAdjustedFluidCost(recipe);
        FluidStack stored = tank.getFluid();
        return !stored.isEmpty() && stored.getFluid() == recipe.fluid() && stored.getAmount() >= required;
    }

    private boolean consumeInputs(GreenhouseRecipe recipe, int fuelSlot, ItemStack fuel) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        int nextEconomyCycle = economyCycle + 1;
        boolean consumeFuelNow = recipe.requiresFuel() && recipe.consumeFuel()
                && nextEconomyCycle >= 1 + economy;

        if (consumeFuelNow && (fuelSlot < 0 || !canStoreRemainder(fuelSlot, fuel))) return false;

        if (recipe.requiresFluid()
                && !(recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA))) {
            int required = getAdjustedFluidCost(recipe);
            int drained = tank.drain(required, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < required) return false;
        }

        if (recipe.requiresFuel() && recipe.consumeFuel()) {
            if (consumeFuelNow) {
                if (!consumeOneFuel(fuelSlot)) return false;
                economyCycle = 0;
            } else {
                economyCycle = nextEconomyCycle;
            }
        }
        return true;
    }

    private boolean canStoreRemainder(int sourceSlot, ItemStack consumed) {
        if (consumed.isEmpty() || !consumed.getItem().hasCraftingRemainingItem()) return true;
        if (items.getStackInSlot(sourceSlot).getCount() == 1) return true;

        ItemStack remainder = new ItemStack(consumed.getItem().getCraftingRemainingItem());
        for (int slot = FIRST_FUEL_SLOT; slot < FIRST_FUEL_SLOT + FUEL_SLOTS; slot++) {
            if (slot == sourceSlot) continue;
            ItemStack existing = items.getStackInSlot(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(existing, remainder)
                    && existing.getCount() + remainder.getCount() <= existing.getMaxStackSize()) return true;
        }
        return false;
    }

    private boolean consumeOneFuel(int slot) {
        if (slot < FIRST_FUEL_SLOT || slot >= FIRST_FUEL_SLOT + FUEL_SLOTS) return false;
        ItemStack stored = items.getStackInSlot(slot);
        if (stored.isEmpty()) return false;

        ItemStack consumed = oneItem(stored);
        ItemStack remainder = consumed.getItem().hasCraftingRemainingItem()
                ? new ItemStack(consumed.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;

        ItemStack updated = stored.copy();
        updated.shrink(1);
        items.setStackInSlot(slot, updated);

        if (remainder.isEmpty()) return true;
        if (updated.isEmpty()) {
            items.setStackInSlot(slot, remainder);
            return true;
        }

        for (int target = FIRST_FUEL_SLOT; target < FIRST_FUEL_SLOT + FUEL_SLOTS; target++) {
            if (target == slot) continue;
            ItemStack existing = items.getStackInSlot(target);
            if (existing.isEmpty()) {
                items.setStackInSlot(target, remainder);
                return true;
            }
            if (ItemStack.isSameItemSameTags(existing, remainder)
                    && existing.getCount() + remainder.getCount() <= existing.getMaxStackSize()) {
                ItemStack merged = existing.copy();
                merged.grow(remainder.getCount());
                items.setStackInSlot(target, merged);
                return true;
            }
        }
        return false;
    }

    private int getAdjustedFluidCost(GreenhouseRecipe recipe) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        return Math.max(1, recipe.fluidAmount() * Math.max(40, 100 - economy * 20) / 100);
    }

    private int getGeneratedMana(GreenhouseRecipe recipe, ItemStack fuel) {
        int base = switch (recipe.fuelMode()) {
            case FURNACE_FUEL -> Math.max(1, Math.min(ENDOFLAME_FUEL_CAP, getBurnTime(fuel)) * 3 / 4);
            case EDIBLE -> getGourmaryllisMana(fuel);
            case SPECIAL_FLOWER -> getRafflowsiaMana(fuel);
            case INGREDIENT -> isNarslimmus() && fuel.is(Items.SLIME_BLOCK)
                    ? Math.max(1, recipe.mana() * 9) : recipe.mana();
            default -> recipe.mana();
        };

        int efficiency = getModuleLevel(GreenhouseModuleItem.Type.EFFICIENCY);
        long value = (long) base * (100 + efficiency * 25) / 100;
        return (int) Math.min(MANA_CAPACITY, Math.max(1L, value));
    }

    private int getBaseOperationTime(GreenhouseRecipe recipe, ItemStack fuel) {
        return switch (recipe.fuelMode()) {
            case FURNACE_FUEL -> Math.max(20, Math.min(ENDOFLAME_FUEL_CAP, getBurnTime(fuel)) / 2);
            case EDIBLE -> Math.max(20, getFoodValue(fuel) * 10);
            default -> recipe.time();
        };
    }

    private static int getBurnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private static @Nullable FoodProperties getFoodProperties(ItemStack stack) {
        return stack.isEmpty() ? null : XplatAbstractions.INSTANCE.getFoodProperties(stack);
    }

    private static int getFoodValue(ItemStack stack) {
        FoodProperties properties = getFoodProperties(stack);
        return properties == null ? 0
                : Math.min(GOURMARYLLIS_MAX_FOOD_VALUE, Math.max(0, properties.getNutrition()));
    }

    private int getGourmaryllisMana(ItemStack food) {
        int value = getFoodValue(food);
        if (value <= 0) return 1;

        int previousIndex = findSameStack(gourmaryllisHistory, food);
        int maxStreak = GOURMARYLLIS_STREAK_MULTIPLIERS.length - 1;
        if (previousIndex < 0) previousIndex = maxStreak;
        int nextStreak = Math.min(gourmaryllisStreak + 1, previousIndex);

        double multiplier;
        if (previousIndex == 0) {
            multiplier = 1.0D / Math.max(1, gourmaryllisRepeatCount + 1);
        } else {
            multiplier = GOURMARYLLIS_STREAK_MULTIPLIERS[
                    Math.max(0, Math.min(maxStreak, nextStreak))];
        }
        return Math.max(1, (int) (value * value * 70.0D * multiplier));
    }

    private void recordGourmaryllisFood(ItemStack food) {
        int previousIndex = findSameStack(gourmaryllisHistory, food);
        int maxStreak = GOURMARYLLIS_STREAK_MULTIPLIERS.length - 1;
        if (previousIndex < 0) previousIndex = maxStreak;

        gourmaryllisStreak = Math.min(gourmaryllisStreak + 1, previousIndex);
        if (previousIndex == 0) {
            gourmaryllisRepeatCount++;
        } else {
            gourmaryllisRepeatCount = 1;
        }

        gourmaryllisHistory.removeIf(existing -> ItemStack.isSameItemSameTags(existing, food));
        gourmaryllisHistory.add(0, oneItem(food));
        while (gourmaryllisHistory.size() > maxStreak) {
            gourmaryllisHistory.remove(gourmaryllisHistory.size() - 1);
        }
    }

    private int getRafflowsiaMana(ItemStack flower) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(flower.getItem());
        int previousIndex = rafflowsiaHistory.indexOf(id);
        int nextStreak = previousIndex < 0
                ? Math.min(47, rafflowsiaStreak + 1)
                : Math.min(rafflowsiaStreak + 1, previousIndex);
        int repeat = previousIndex == 0 ? rafflowsiaRepeatCount + 1 : 1;
        long varied = 2_000L + (long) nextStreak * nextStreak * 600L;
        return (int) Math.max(1L, Math.min(553_702L, varied / repeat));
    }

    private void recordRafflowsiaFlower(ItemStack flower) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(flower.getItem());
        int previousIndex = rafflowsiaHistory.indexOf(id);
        rafflowsiaStreak = previousIndex < 0
                ? Math.min(47, rafflowsiaStreak + 1)
                : Math.min(rafflowsiaStreak + 1, previousIndex);
        rafflowsiaRepeatCount = previousIndex == 0 ? rafflowsiaRepeatCount + 1 : 1;

        rafflowsiaHistory.remove(id);
        rafflowsiaHistory.add(0, id);
        while (rafflowsiaHistory.size() > 48) {
            rafflowsiaHistory.remove(rafflowsiaHistory.size() - 1);
        }
    }

    private static int findSameStack(List<ItemStack> stacks, ItemStack target) {
        for (int index = 0; index < stacks.size(); index++) {
            if (ItemStack.isSameItemSameTags(stacks.get(index), target)) return index;
        }
        return -1;
    }

    private void afterSuccessfulCycle(GreenhouseRecipe recipe, ItemStack fuel) {
        switch (recipe.fuelMode()) {
            case EDIBLE -> recordGourmaryllisFood(fuel);
            case WOOL_CYCLE -> spectrolusNextColor =
                    spectrolusNextColor >= DyeColor.BLACK.getId()
                            ? DyeColor.WHITE.getId() : DyeColor.byId(spectrolusNextColor + 1).getId();
            case SPECIAL_FLOWER -> recordRafflowsiaFlower(fuel);
            default -> {
            }
        }
    }

    private Item getExpectedWoolItem() {
        return switch (DyeColor.byId(spectrolusNextColor)) {
            case WHITE -> Items.WHITE_WOOL;
            case ORANGE -> Items.ORANGE_WOOL;
            case MAGENTA -> Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case YELLOW -> Items.YELLOW_WOOL;
            case LIME -> Items.LIME_WOOL;
            case PINK -> Items.PINK_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case CYAN -> Items.CYAN_WOOL;
            case PURPLE -> Items.PURPLE_WOOL;
            case BLUE -> Items.BLUE_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GREEN -> Items.GREEN_WOOL;
            case RED -> Items.RED_WOOL;
            case BLACK -> Items.BLACK_WOOL;
        };
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

    private boolean isSupportedFlowerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("botania".equals(id.getNamespace())
                && STANDARD_FLOWERS.contains(normalizeFlowerPath(id.getPath()))) return true;
        if (level != null) {
            for (GreenhouseRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.GREENHOUSE_TYPE.get())) {
                if (recipe.matchesFlower(stack)) return true;
            }
        }
        return false;
    }

    private int getModeId() {
        ItemStack flower = items.getStackInSlot(FLOWER_SLOT);
        if (flower.isEmpty()) return 0;
        String path = normalizeFlowerPath(BuiltInRegistries.ITEM.getKey(flower.getItem()).getPath());
        return switch (path) {
            case "endoflame" -> 1;
            case "hydroangeas" -> 2;
            case "gourmaryllis" -> 3;
            case "entropinnyum" -> 4;
            case "thermalily" -> 5;
            case "spectrolus" -> 6;
            case "rosa_arcana" -> 7;
            case "munchdew" -> 8;
            case "kekimurus" -> 9;
            case "narslimmus" -> 10;
            case "shulk_me_not" -> 11;
            case "dandelifeon" -> 12;
            case "rafflowsia" -> 13;
            default -> activeRecipeId == null ? 0 : 14;
        };
    }

    private boolean isNarslimmus() {
        ItemStack flower = items.getStackInSlot(FLOWER_SLOT);
        return !flower.isEmpty() && "narslimmus".equals(normalizeFlowerPath(
                BuiltInRegistries.ITEM.getKey(flower.getItem()).getPath()));
    }

    private static String normalizeFlowerPath(String path) {
        return path.startsWith("floating_") ? path.substring("floating_".length()) : path;
    }

    private static ItemStack oneItem(ItemStack source) {
        ItemStack result = source.copy();
        result.setCount(source.isEmpty() ? 0 : 1);
        return result;
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
        tag.putInt("SpectrolusNextColor", spectrolusNextColor);
        tag.putInt("GourmaryllisStreak", gourmaryllisStreak);
        tag.putInt("GourmaryllisRepeat", gourmaryllisRepeatCount);
        tag.putInt("RafflowsiaStreak", rafflowsiaStreak);
        tag.putInt("RafflowsiaRepeat", rafflowsiaRepeatCount);
        if (activeRecipeId != null) tag.putString("ActiveRecipe", activeRecipeId.toString());

        ListTag foods = new ListTag();
        for (ItemStack food : gourmaryllisHistory) {
            foods.add(food.save(new CompoundTag()));
        }
        tag.put("GourmaryllisFoods", foods);

        ListTag flowers = new ListTag();
        for (ResourceLocation flower : rafflowsiaHistory) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", flower.toString());
            flowers.add(entry);
        }
        tag.put("RafflowsiaFlowers", flowers);
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
        spectrolusNextColor = DyeColor.byId(tag.getInt("SpectrolusNextColor")).getId();
        gourmaryllisStreak = tag.contains("GourmaryllisStreak") ? tag.getInt("GourmaryllisStreak") : -1;
        gourmaryllisRepeatCount = Math.max(0, tag.getInt("GourmaryllisRepeat"));
        rafflowsiaStreak = tag.contains("RafflowsiaStreak") ? tag.getInt("RafflowsiaStreak") : -1;
        rafflowsiaRepeatCount = Math.max(0, tag.getInt("RafflowsiaRepeat"));
        activeRecipeId = tag.contains("ActiveRecipe")
                ? ResourceLocation.tryParse(tag.getString("ActiveRecipe")) : null;

        gourmaryllisHistory.clear();
        ListTag foods = tag.getList("GourmaryllisFoods", Tag.TAG_COMPOUND);
        for (int index = 0; index < foods.size(); index++) {
            ItemStack food = ItemStack.of(foods.getCompound(index));
            if (!food.isEmpty()) gourmaryllisHistory.add(oneItem(food));
        }

        rafflowsiaHistory.clear();
        ListTag flowers = tag.getList("RafflowsiaFlowers", Tag.TAG_COMPOUND);
        for (int index = 0; index < flowers.size(); index++) {
            ResourceLocation flower = ResourceLocation.tryParse(flowers.getCompound(index).getString("Id"));
            if (flower != null) rafflowsiaHistory.add(flower);
        }
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
