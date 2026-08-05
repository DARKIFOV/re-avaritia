from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> tuple[Path, str]:
    target = ROOT / path
    return target, target.read_text(encoding="utf-8")


def write(target: Path, text: str) -> None:
    target.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_all(text: str, old: str, new: str) -> str:
    return text.replace(old, new)


def patch_greenhouse() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/GreenhouseBlockEntity.java")
    text = replace_once(text, "    public static final int MODULE_SLOTS = 3;",
                        "    public static final int MODULE_SLOTS = 4;", "greenhouse module slots")
    text = replace_once(text, "    public static final int FIRST_FUEL_SLOT = 4;",
                        "    public static final int FIRST_FUEL_SLOT = 5;", "greenhouse fuel start")
    text = replace_once(text, "    public static final int TOTAL_SLOTS = 10;",
                        "    public static final int TOTAL_SLOTS = 11;", "greenhouse total slots")
    text = replace_once(text, "            if (slot == FLOWER_SLOT) return 64;",
                        "            if (slot == FLOWER_SLOT) return 16;", "greenhouse flower limit")
    text = replace_all(text, "int flowerCount = Math.max(1, flower.getCount());",
                       "int flowerCount = Math.max(1, Math.min(16, flower.getCount()));")

    lava_check = "recipe.fluid() == Fluids.LAVA && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA)"
    text = replace_all(text, lava_check, "hasInfiniteFluidModule(recipe)")

    marker = "    private boolean hasRequiredFluid(GreenhouseRecipe recipe) {\n"
    helper = """    private boolean hasInfiniteFluidModule(GreenhouseRecipe recipe) {
        return (recipe.fluid() == Fluids.WATER
                && hasModule(GreenhouseModuleItem.Type.INFINITE_WATER))
                || (recipe.fluid() == Fluids.LAVA
                && hasModule(GreenhouseModuleItem.Type.INFINITE_LAVA));
    }

"""
    if helper not in text:
        if marker not in text:
            raise RuntimeError("greenhouse infinite-fluid helper marker missing")
        text = text.replace(marker, helper + marker, 1)

    old_load = "        items.deserializeNBT(tag.getCompound(\"Inventory\"));\n"
    new_load = "        loadInventory(tag.getCompound(\"Inventory\"));\n"
    text = replace_once(text, old_load, new_load, "greenhouse inventory migration call")

    capability_marker = "    @Override public <T> @NotNull LazyOptional<T> getCapability(\n"
    migration = """    private void loadInventory(CompoundTag inventoryTag) {
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(inventoryTag);
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        if (loaded.getSlots() == 10) {
            for (int slot = 0; slot < 4; slot++) {
                items.setStackInSlot(slot, loaded.getStackInSlot(slot).copy());
            }
            for (int oldFuel = 4; oldFuel < 10; oldFuel++) {
                items.setStackInSlot(oldFuel + 1, loaded.getStackInSlot(oldFuel).copy());
            }
            return;
        }
        for (int slot = 0; slot < Math.min(TOTAL_SLOTS, loaded.getSlots()); slot++) {
            items.setStackInSlot(slot, loaded.getStackInSlot(slot).copy());
        }
    }

"""
    if migration not in text:
        if capability_marker not in text:
            raise RuntimeError("greenhouse migration marker missing")
        text = text.replace(capability_marker, migration + capability_marker, 1)
    write(target, text)


def patch_mana_drill() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/ManaDrillBlockEntity.java")
    text = replace_once(text, "    public static final int OUTPUT_SLOTS = 27;",
                        "    public static final int OUTPUT_SLOTS = 81;", "mana drill output slots")
    text = replace_once(text, "        items.deserializeNBT(tag.getCompound(\"Inventory\"));\n",
                        "        loadInventory(tag.getCompound(\"Inventory\"));\n",
                        "mana drill inventory migration call")
    marker = "    @Override public <T> @NotNull LazyOptional<T> getCapability(\n"
    migration = """    private void loadInventory(CompoundTag inventoryTag) {
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(inventoryTag);
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < Math.min(TOTAL_SLOTS, loaded.getSlots()); slot++) {
            items.setStackInSlot(slot, loaded.getStackInSlot(slot).copy());
        }
    }

"""
    if migration not in text:
        if marker not in text:
            raise RuntimeError("mana drill migration marker missing")
        text = text.replace(marker, migration + marker, 1)
    write(target, text)


def patch_luck_converter() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/LuckConverterBlockEntity.java")
    text = replace_once(text, "    public static final int MAX_OUTPUTS = 60;",
                        "    public static final int MAX_OUTPUTS = 81;", "luck output capacity")
    text = replace_once(text, "    public static final int MODULE_SLOT = 132;",
                        "    public static final int MODULE_SLOT = 153;", "luck module slot")
    text = replace_once(text, "    public static final int FIRST_UPGRADE = 133;",
                        "    public static final int FIRST_UPGRADE = 154;", "luck upgrade start")
    text = replace_once(text, "    public static final int ENERGY_SLOT = 139;",
                        "    public static final int ENERGY_SLOT = 160;", "luck energy slot")
    text = replace_once(text, "    public static final int TOTAL_SLOTS = 140;",
                        "    public static final int TOTAL_SLOTS = 161;", "luck total slots")
    text = replace_once(text, "    public int outputSlots() { return isAdvanced() ? 60 : 30; }",
                        "    public int outputSlots() { return 81; }", "luck output slot count")
    text = replace_once(text, "        items.deserializeNBT(tag.getCompound(\"Inventory\"));\n",
                        "        loadInventory(tag.getCompound(\"Inventory\"));\n",
                        "luck inventory migration call")
    marker = "    @Override\n    public <T> @NotNull LazyOptional<T> getCapability(\n"
    migration = """    private void loadInventory(CompoundTag inventoryTag) {
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(inventoryTag);
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
        if (loaded.getSlots() == 140) {
            for (int slot = 0; slot < 132; slot++) {
                items.setStackInSlot(slot, loaded.getStackInSlot(slot).copy());
            }
            items.setStackInSlot(MODULE_SLOT, loaded.getStackInSlot(132).copy());
            for (int index = 0; index < UPGRADE_SLOTS; index++) {
                items.setStackInSlot(FIRST_UPGRADE + index,
                        loaded.getStackInSlot(133 + index).copy());
            }
            items.setStackInSlot(ENERGY_SLOT, loaded.getStackInSlot(139).copy());
            return;
        }
        for (int slot = 0; slot < Math.min(TOTAL_SLOTS, loaded.getSlots()); slot++) {
            items.setStackInSlot(slot, loaded.getStackInSlot(slot).copy());
        }
    }

"""
    if migration not in text:
        if marker not in text:
            raise RuntimeError("luck migration marker missing")
        text = text.replace(marker, migration + marker, 1)
    write(target, text)


def patch_molecular_assembler() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/MolecularAssemblerBlockEntity.java")
    old_time = """    private int adjustedCraftTime(int baseTime) {
        int multiplier = 1 + getAe2SpeedCardCount();
        return Math.max(1, (Math.max(1, baseTime) + multiplier - 1) / multiplier);
    }

    private int adjustedEnergyPerTick(int baseEnergy) {
        if (baseEnergy <= 0) return 0;
        long adjusted = (long) baseEnergy * (1 + getAe2SpeedCardCount());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, adjusted));
    }
"""
    new_time = """    private int speedMultiplier() {
        for (int slot = 0; slot < AE2_SPEED_CARD_SLOTS; slot++) {
            ItemStack stack = ae2SpeedCards.getStackInSlot(slot);
            if (isSuperSpeedCard(stack)) return 512;
        }
        return 4 + getAe2SpeedCardCount();
    }

    private int adjustedCraftTime(int baseTime) {
        int multiplier = Math.max(1, speedMultiplier());
        return Math.max(1, (Math.max(1, baseTime) + multiplier - 1) / multiplier);
    }

    private int adjustedEnergyPerTick(int baseEnergy) {
        if (baseEnergy <= 0) return 0;
        long adjusted = (long) baseEnergy * Math.max(1, speedMultiplier());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, adjusted));
    }
"""
    text = replace_once(text, old_time, new_time, "assembler speed multiplier")
    old_card = """    public static boolean isAe2SpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return \"ae2\".equals(id.getNamespace()) && \"speed_card\".equals(id.getPath());
    }
"""
    new_card = """    public static boolean isAe2SpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (\"ae2\".equals(id.getNamespace()) && \"speed_card\".equals(id.getPath()))
                || (\"ae2_overclocked\".equals(id.getNamespace())
                && \"super_speed_card\".equals(id.getPath()));
    }

    private static boolean isSuperSpeedCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return \"ae2_overclocked\".equals(id.getNamespace())
                && \"super_speed_card\".equals(id.getPath());
    }
"""
    text = replace_once(text, old_card, new_card, "assembler super speed card")
    write(target, text)


def patch_encoder() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremePatternEncoderBlockEntity.java")
    text = replace_once(text, "    private boolean ghostGrid;",
                        "    private boolean ghostGrid = true;", "encoder ghost-grid default")
    text = replace_once(text, "        ghostGrid = tag.getBoolean(\"GhostGrid\");",
                        "        ghostGrid = !tag.contains(\"GhostGrid\") || tag.getBoolean(\"GhostGrid\");",
                        "encoder ghost-grid load default")
    write(target, text)


def patch_mod_items() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/registry/ModItems.java")
    lava_decl = """    public static final RegistryObject<Item> GREENHOUSE_INFINITE_LAVA_MODULE = greenhouseModule(
            \"greenhouse_infinite_lava_module\", GreenhouseModuleItem.Type.INFINITE_LAVA, 1);
"""
    water_decl = """    public static final RegistryObject<Item> GREENHOUSE_INFINITE_WATER_MODULE = greenhouseModule(
            \"greenhouse_infinite_water_module\", GreenhouseModuleItem.Type.INFINITE_WATER, 1);
"""
    if water_decl not in text:
        if lava_decl not in text:
            raise RuntimeError("greenhouse infinite-lava registration missing")
        text = text.replace(lava_decl, water_decl + lava_decl, 1)
    old_list = """                GREENHOUSE_SPEED_MODULE_1, GREENHOUSE_SPEED_MODULE_2, GREENHOUSE_SPEED_MODULE_3,
                GREENHOUSE_INFINITE_DAY_MODULE, GREENHOUSE_INFINITE_NIGHT_MODULE, GREENHOUSE_INFINITE_LAVA_MODULE);
"""
    new_list = """                GREENHOUSE_SPEED_MODULE_1, GREENHOUSE_SPEED_MODULE_2, GREENHOUSE_SPEED_MODULE_3,
                GREENHOUSE_INFINITE_DAY_MODULE, GREENHOUSE_INFINITE_NIGHT_MODULE,
                GREENHOUSE_INFINITE_WATER_MODULE, GREENHOUSE_INFINITE_LAVA_MODULE);
"""
    text = replace_once(text, old_list, new_list, "greenhouse module item list")
    write(target, text)


if __name__ == "__main__":
    patch_greenhouse()
    patch_mana_drill()
    patch_luck_converter()
    patch_molecular_assembler()
    patch_encoder()
    patch_mod_items()
    print("Applied 0.6.68 full source synchronization patches")
