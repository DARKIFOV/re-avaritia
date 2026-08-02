from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def patch_greenhouse() -> None:
    path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/GreenhouseBlockEntity.java"
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "    public static final int FLUID_CAPACITY = 8_000;",
        "    public static final int FLUID_CAPACITY = 64_000;",
        "greenhouse fluid capacity",
    )

    old_tick = '''        int fuelSlot = recipe.requiresFuel() ? findFuelSlot(recipe) : -1;
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
'''
    new_tick = '''        int flowerCount = Math.max(1, flower.getCount());
        int fuelSlot = recipe.requiresFuel() ? findFuelSlot(recipe) : -1;
        if (recipe.requiresFuel() && fuelSlot < 0) {
            resetProgress(false);
            setStatus(STATUS_NO_FUEL);
            return;
        }
        if (!hasRequiredFluid(recipe, flowerCount)) {
            resetProgress(false);
            setStatus(STATUS_NO_FLUID);
            return;
        }

        ItemStack fuel = fuelSlot < 0 ? ItemStack.EMPTY : oneItem(items.getStackInSlot(fuelSlot));
        if (!hasFuelForBatch(recipe, fuel, flowerCount)) {
            resetProgress(false);
            setStatus(STATUS_NO_FUEL);
            return;
        }
        int generatedMana = getGeneratedManaForBatch(recipe, fuel, flowerCount);
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
        if (!consumeInputs(recipe, fuelSlot, fuel, flowerCount)) {
            resetProgress(false);
            setStatus(recipe.requiresFluid() && !hasRequiredFluid(recipe, flowerCount)
                    ? STATUS_NO_FLUID : STATUS_NO_FUEL);
            return;
        }

        afterSuccessfulCycle(recipe, fuel);
        receiveMana(generatedMana);
'''
    text = replace_once(text, old_tick, new_tick, "greenhouse stacked tick")

    old_fluid = '''    private boolean hasRequiredFluid(GreenhouseRecipe recipe) {
        if (!recipe.requiresFluid()) return true;
        if (recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA)) return true;
        int required = getAdjustedFluidCost(recipe);
        FluidStack stored = tank.getFluid();
        return !stored.isEmpty() && stored.getFluid() == recipe.fluid() && stored.getAmount() >= required;
    }
'''
    new_fluid = '''    private boolean hasRequiredFluid(GreenhouseRecipe recipe) {
        return hasRequiredFluid(recipe, 1);
    }

    private boolean hasRequiredFluid(GreenhouseRecipe recipe, int flowerCount) {
        if (!recipe.requiresFluid()) return true;
        if (recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA)) return true;
        int required = getAdjustedFluidCost(recipe, flowerCount);
        FluidStack stored = tank.getFluid();
        return !stored.isEmpty() && stored.getFluid() == recipe.fluid() && stored.getAmount() >= required;
    }
'''
    text = replace_once(text, old_fluid, new_fluid, "greenhouse fluid batch check")

    old_consume = '''    private boolean consumeInputs(GreenhouseRecipe recipe, int fuelSlot, ItemStack fuel) {
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
'''
    new_consume = '''    private boolean consumeInputs(GreenhouseRecipe recipe, int fuelSlot, ItemStack fuel,
                                  int flowerCount) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        int nextEconomyCycle = economyCycle + 1;
        boolean consumeFuelNow = recipe.requiresFuel() && recipe.consumeFuel()
                && nextEconomyCycle >= 1 + economy;

        int fuelUnits = consumeFuelNow ? Math.max(1, flowerCount) : 0;
        if (consumeFuelNow && (fuelSlot < 0 || !canConsumeFuelBatch(fuel, fuelUnits))) return false;

        if (recipe.requiresFluid()
                && !(recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA))) {
            int required = getAdjustedFluidCost(recipe, flowerCount);
            int drained = tank.drain(required, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained < required) return false;
        }

        if (recipe.requiresFuel() && recipe.consumeFuel()) {
            if (consumeFuelNow) {
                if (!consumeFuelBatch(fuel, fuelUnits)) return false;
                economyCycle = 0;
            } else {
                economyCycle = nextEconomyCycle;
            }
        }
        return true;
    }
'''
    text = replace_once(text, old_consume, new_consume, "greenhouse consume batch")

    insert_before_adjusted = '''    private int getAdjustedFluidCost(GreenhouseRecipe recipe) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        return Math.max(1, recipe.fluidAmount() * Math.max(40, 100 - economy * 20) / 100);
    }
'''
    batch_helpers = '''    private boolean hasFuelForBatch(GreenhouseRecipe recipe, ItemStack fuel, int flowerCount) {
        if (!recipe.requiresFuel() || !recipe.consumeFuel()) return true;
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        boolean consumeFuelNow = economyCycle + 1 >= 1 + economy;
        return !consumeFuelNow || countMatchingFuel(fuel) >= Math.max(1, flowerCount);
    }

    private int countMatchingFuel(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;
        int count = 0;
        for (int slot = FIRST_FUEL_SLOT; slot < FIRST_FUEL_SLOT + FUEL_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, fuel)) count += stack.getCount();
        }
        return count;
    }

    private boolean canConsumeFuelBatch(ItemStack fuel, int amount) {
        if (fuel.isEmpty() || amount <= 0 || countMatchingFuel(fuel) < amount) return false;
        if (!fuel.getItem().hasCraftingRemainingItem()) return true;

        ItemStackHandler simulation = new ItemStackHandler(FUEL_SLOTS);
        for (int index = 0; index < FUEL_SLOTS; index++) {
            simulation.setStackInSlot(index, items.getStackInSlot(FIRST_FUEL_SLOT + index).copy());
        }
        int remaining = amount;
        for (int index = 0; index < FUEL_SLOTS && remaining > 0; index++) {
            ItemStack stack = simulation.getStackInSlot(index);
            if (!ItemStack.isSameItemSameTags(stack, fuel)) continue;
            int remove = Math.min(remaining, stack.getCount());
            ItemStack updated = stack.copy();
            updated.shrink(remove);
            simulation.setStackInSlot(index, updated);
            remaining -= remove;
        }
        if (remaining > 0) return false;

        ItemStack remainder = new ItemStack(fuel.getItem().getCraftingRemainingItem(), amount);
        for (int index = 0; index < FUEL_SLOTS && !remainder.isEmpty(); index++) {
            remainder = simulation.insertItem(index, remainder, false);
        }
        return remainder.isEmpty();
    }

    private boolean consumeFuelBatch(ItemStack fuel, int amount) {
        if (!canConsumeFuelBatch(fuel, amount)) return false;
        int remaining = amount;
        for (int slot = FIRST_FUEL_SLOT;
             slot < FIRST_FUEL_SLOT + FUEL_SLOTS && remaining > 0; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameTags(stack, fuel)) continue;
            int remove = Math.min(remaining, stack.getCount());
            ItemStack updated = stack.copy();
            updated.shrink(remove);
            items.setStackInSlot(slot, updated);
            remaining -= remove;
        }
        if (remaining > 0) return false;

        if (fuel.getItem().hasCraftingRemainingItem()) {
            ItemStack remainder = new ItemStack(fuel.getItem().getCraftingRemainingItem(), amount);
            for (int slot = FIRST_FUEL_SLOT;
                 slot < FIRST_FUEL_SLOT + FUEL_SLOTS && !remainder.isEmpty(); slot++) {
                remainder = items.insertItem(slot, remainder, false);
            }
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }

    private int getAdjustedFluidCost(GreenhouseRecipe recipe) {
        return getAdjustedFluidCost(recipe, 1);
    }

    private int getAdjustedFluidCost(GreenhouseRecipe recipe, int flowerCount) {
        int economy = getModuleLevel(GreenhouseModuleItem.Type.ECONOMY);
        long unitCost = Math.max(1L,
                (long) recipe.fluidAmount() * Math.max(40, 100 - economy * 20) / 100L);
        return (int) Math.min(Integer.MAX_VALUE, unitCost * Math.max(1, flowerCount));
    }
'''
    text = replace_once(text, insert_before_adjusted, batch_helpers, "greenhouse batch helpers")

    old_generated_end = '''        long value = (long) base * (100 + efficiency * 25) / 100;
        return (int) Math.min(MANA_CAPACITY, Math.max(1L, value));
    }

    private int getBaseOperationTime'''
    new_generated_end = '''        long value = (long) base * (100 + efficiency * 25) / 100;
        return (int) Math.min(MANA_CAPACITY, Math.max(1L, value));
    }

    private int getGeneratedManaForBatch(GreenhouseRecipe recipe, ItemStack fuel, int flowerCount) {
        long total = (long) getGeneratedMana(recipe, fuel) * Math.max(1, flowerCount);
        return (int) Math.min(MANA_CAPACITY, Math.max(1L, total));
    }

    private int getBaseOperationTime'''
    text = replace_once(text, old_generated_end, new_generated_end, "greenhouse generated mana batch")

    path.write_text(text, encoding="utf-8")


def patch_luck_converter() -> None:
    path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/LuckConverterBlockEntity.java"
    text = path.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "    private int status;\n",
        "    private int status;\n    private int nextInputSlot;\n",
        "luck input cursor field",
    )

    old_collect = '''    private List<PendingInput> collectWork() {
        List<PendingInput> work = new ArrayList<>();
        int amount = operationsPerInput();
        for (int slot = 0; slot < inputSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!isProcessableInput(stack)) continue;
            work.add(new PendingInput(slot, Math.min(amount, stack.getCount()), stack.copy()));
        }
        return work;
    }
'''
    new_collect = '''    private List<PendingInput> collectWork() {
        List<PendingInput> work = new ArrayList<>();
        int slots = inputSlots();
        int amount = operationsPerInput();
        for (int offset = 0; offset < slots; offset++) {
            int slot = Math.floorMod(nextInputSlot + offset, slots);
            ItemStack stack = items.getStackInSlot(slot);
            if (!isProcessableInput(stack)) continue;
            work.add(new PendingInput(slot, Math.min(amount, stack.getCount()), stack.copy()));
            break;
        }
        return work;
    }

    private void advanceInputCursor(List<PendingInput> work) {
        if (work.isEmpty()) return;
        nextInputSlot = Math.floorMod(work.get(0).slot() + 1, inputSlots());
    }
'''
    text = replace_once(text, old_collect, new_collect, "luck bounded parallel work")

    old_success = '''        for (PendingInput input : work) {
            items.extractItem(input.slot(), input.amount(), false);
        }
        for (ItemStack stack : results) insertOutput(stack, false);
        progress = 0;
'''
    new_success = '''        for (PendingInput input : work) {
            items.extractItem(input.slot(), input.amount(), false);
        }
        for (ItemStack stack : results) insertOutput(stack, false);
        advanceInputCursor(work);
        progress = 0;
'''
    text = replace_once(text, old_success, new_success, "luck advance input cursor")

    text = replace_once(
        text,
        '        tag.putInt("Status", status);\n',
        '        tag.putInt("Status", status);\n        tag.putInt("NextInputSlot", nextInputSlot);\n',
        "luck save cursor",
    )
    text = replace_once(
        text,
        '        status = tag.getInt("Status");\n',
        '        status = tag.getInt("Status");\n        nextInputSlot = Math.max(0, tag.getInt("NextInputSlot"));\n',
        "luck load cursor",
    )

    path.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    patch_greenhouse()
    patch_luck_converter()
    print("Applied 0.6.8 greenhouse stack and luck parallel-operation fixes")
