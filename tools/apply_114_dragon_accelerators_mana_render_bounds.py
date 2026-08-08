from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


# ---------------------------------------------------------------------------
# Extreme Dragon Assembler: 4 AE2 accelerator slots, mirroring the 9x9
# Molecular Assembler's speed-card behavior without changing the historical
# 62-slot main inventory layout.
# ---------------------------------------------------------------------------
be_path = "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"

patch(
    be_path,
    "    public static final int PATTERN_COUNT = 36;\n    public static final int TOTAL_SLOTS = 62;\n",
    "    public static final int PATTERN_COUNT = 36;\n"
    "    public static final int SPEED_CARD_SLOTS = 4;\n"
    "    public static final int TOTAL_SLOTS = 62;\n",
    "dragon assembler speed-card constant",
)

patch(
    be_path,
    "        @Override protected void onContentsChanged(int slot) { setChanged(); }\n"
    "    };\n"
    "    private final EnergyStorage energy = ",
    "        @Override protected void onContentsChanged(int slot) { setChanged(); }\n"
    "    };\n"
    "    private final ItemStackHandler speedCards = new ItemStackHandler(SPEED_CARD_SLOTS) {\n"
    "        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {\n"
    "            return MolecularAssemblerBlockEntity.isAe2SpeedCard(stack);\n"
    "        }\n"
    "        @Override public int getSlotLimit(int slot) { return 1; }\n"
    "        @Override protected void onContentsChanged(int slot) { setChanged(); }\n"
    "    };\n"
    "    private final EnergyStorage energy = ",
    "dragon assembler speed-card handler",
)

patch(
    be_path,
    "                case 1 -> CRAFT_TICKS;\n",
    "                case 1 -> adjustedCraftTicks();\n",
    "dragon assembler synced craft time",
)

patch(
    be_path,
    "        long targetSpent = (view.energy() * (progress + 1L)) / CRAFT_TICKS;\n",
    "        int craftTicks = adjustedCraftTicks();\n"
    "        long targetSpent = (view.energy() * (progress + 1L)) / craftTicks;\n",
    "dragon assembler accelerated energy distribution",
)

patch(
    be_path,
    "        if (progress >= CRAFT_TICKS) finishCraft(view, tier);\n",
    "        if (progress >= craftTicks) finishCraft(view, tier);\n",
    "dragon assembler accelerated completion",
)

patch(
    be_path,
    "    private Optional<DragonFusionSupport.View> findCraftablePattern(int tier) {\n",
    "    private int speedMultiplier() {\n"
    "        for (int slot = 0; slot < SPEED_CARD_SLOTS; slot++) {\n"
    "            ItemStack stack = speedCards.getStackInSlot(slot);\n"
    "            if (isSuperSpeedCard(stack)) return 512;\n"
    "        }\n"
    "        return 4 + getSpeedCardCount();\n"
    "    }\n\n"
    "    private int adjustedCraftTicks() {\n"
    "        int multiplier = Math.max(1, speedMultiplier());\n"
    "        return Math.max(1, (CRAFT_TICKS + multiplier - 1) / multiplier);\n"
    "    }\n\n"
    "    public int getSpeedCardCount() {\n"
    "        int count = 0;\n"
    "        for (int slot = 0; slot < SPEED_CARD_SLOTS; slot++) {\n"
    "            if (!speedCards.getStackInSlot(slot).isEmpty()) count++;\n"
    "        }\n"
    "        return count;\n"
    "    }\n\n"
    "    private static boolean isSuperSpeedCard(ItemStack stack) {\n"
    "        if (stack.isEmpty()) return false;\n"
    "        net.minecraft.resources.ResourceLocation id =\n"
    "                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());\n"
    "        return \"ae2_overclocked\".equals(id.getNamespace())\n"
    "                && \"super_speed_card\".equals(id.getPath());\n"
    "    }\n\n"
    "    private Optional<DragonFusionSupport.View> findCraftablePattern(int tier) {\n",
    "dragon assembler speed multiplier methods",
)

patch(
    be_path,
    "        tag.put(\"Inventory\", items.serializeNBT());\n",
    "        tag.put(\"Inventory\", items.serializeNBT());\n"
    "        tag.put(\"SpeedCards\", speedCards.serializeNBT());\n",
    "dragon assembler save speed cards",
)

patch(
    be_path,
    "        items.deserializeNBT(tag.getCompound(\"Inventory\"));\n",
    "        items.deserializeNBT(tag.getCompound(\"Inventory\"));\n"
    "        if (tag.contains(\"SpeedCards\")) speedCards.deserializeNBT(tag.getCompound(\"SpeedCards\"));\n",
    "dragon assembler load speed cards",
)

patch(
    be_path,
    "    public ItemStackHandler getItems() { return items; }\n",
    "    public ItemStackHandler getItems() { return items; }\n"
    "    public ItemStackHandler getSpeedCards() { return speedCards; }\n",
    "dragon assembler speed-card getter",
)

patch(
    be_path,
    "        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {\n"
    "            ItemStack stack = items.getStackInSlot(slot);\n"
    "            if (!stack.isEmpty()) drops.add(stack.copy());\n"
    "        }\n"
    "        return drops;\n",
    "        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {\n"
    "            ItemStack stack = items.getStackInSlot(slot);\n"
    "            if (!stack.isEmpty()) drops.add(stack.copy());\n"
    "        }\n"
    "        for (int slot = 0; slot < SPEED_CARD_SLOTS; slot++) {\n"
    "            ItemStack stack = speedCards.getStackInSlot(slot);\n"
    "            if (!stack.isEmpty()) drops.add(stack.copy());\n"
    "        }\n"
    "        return drops;\n",
    "dragon assembler speed-card drops",
)

# Menu: append four speed-card slots after the 62 historical machine slots.
menu_path = "src/main/java/ru/rfvv/metatechreborn/menu/ExtremeDragonAssemblerMenu.java"
patch(
    menu_path,
    "    public static final int PLAYER_START = ExtremeDragonAssemblerBlockEntity.TOTAL_SLOTS;\n",
    "    public static final int PLAYER_START = ExtremeDragonAssemblerBlockEntity.TOTAL_SLOTS\n"
    "            + ExtremeDragonAssemblerBlockEntity.SPEED_CARD_SLOTS;\n",
    "dragon assembler player start after speed cards",
)

# Final 0.6.111 menu uses coordinates centered one pixel inside the painted slots.
patch(
    menu_path,
    "        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT; i++) {\n"
    "            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.PATTERN_START + i,\n"
    "                    223 + (i % 9) * 18, 35 + (i / 9) * 18));\n"
    "        }\n\n",
    "        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.PATTERN_COUNT; i++) {\n"
    "            addSlot(new SlotItemHandler(blockEntity.getItems(), ExtremeDragonAssemblerBlockEntity.PATTERN_START + i,\n"
    "                    223 + (i % 9) * 18, 35 + (i / 9) * 18));\n"
    "        }\n"
    "        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.SPEED_CARD_SLOTS; i++) {\n"
    "            addSlot(new SlotItemHandler(blockEntity.getSpeedCards(), i, 223 + i * 20, 119) {\n"
    "                @Override public boolean mayPlace(@NotNull ItemStack stack) {\n"
    "                    return MolecularAssemblerBlockEntity.isAe2SpeedCard(stack);\n"
    "                }\n"
    "            });\n"
    "        }\n\n",
    "dragon assembler menu speed-card slots",
)

# Add import for the public 9x9 speed-card validator.
patch(
    menu_path,
    "import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;\n",
    "import ru.rfvv.metatechreborn.blockentity.ExtremeDragonAssemblerBlockEntity;\n"
    "import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;\n",
    "dragon assembler menu molecular import",
)

patch(
    menu_path,
    "        } else if (DragonFusionSupport.isInjector(stack)) {\n",
    "        } else if (MolecularAssemblerBlockEntity.isAe2SpeedCard(stack)) {\n"
    "            if (!moveItemStackTo(stack, ExtremeDragonAssemblerBlockEntity.TOTAL_SLOTS,\n"
    "                    PLAYER_START, false)) return ItemStack.EMPTY;\n"
    "        } else if (DragonFusionSupport.isInjector(stack)) {\n",
    "dragon assembler shift-click speed cards",
)

# Screen: extend the pattern-bank panel and draw four accelerator slots.
screen_path = "src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java"
patch(
    screen_path,
    "        panel(g, 220, 20, 164, 94);\n",
    "        panel(g, 220, 20, 164, 136);\n",
    "dragon assembler taller pattern panel",
)
patch(
    screen_path,
    "        for (int i = 0; i < 36; i++) slot(g, 222 + (i % 9) * 18, 34 + (i / 9) * 18, 0xFFAA2828);\n",
    "        for (int i = 0; i < 36; i++) slot(g, 222 + (i % 9) * 18, 34 + (i / 9) * 18, 0xFFAA2828);\n"
    "        for (int i = 0; i < ExtremeDragonAssemblerBlockEntity.SPEED_CARD_SLOTS; i++)\n"
    "            slot(g, 222 + i * 20, 118, 0xFF8E44AD);\n",
    "dragon assembler speed-card slot backgrounds",
)
patch(
    screen_path,
    "        g.drawString(font, Component.literal(\"Шаблоны\"), 228, 24, TEXT, false);\n",
    "        g.drawString(font, Component.literal(\"Шаблоны\"), 228, 24, TEXT, false);\n"
    "        g.drawString(font, Component.literal(\"Ускорители\"), 228, 106, 0xFFFFB0FF, false);\n",
    "dragon assembler accelerator label",
)

# ---------------------------------------------------------------------------
# Mana Drill: the shaped renderer extends several blocks beyond the controller.
# Give the block entity an explicit large render bounding box so Minecraft never
# culls the entire renderer at oblique camera angles while visible geometry is
# still on screen.
# ---------------------------------------------------------------------------
mana_path = "src/main/java/ru/rfvv/metatechreborn/blockentity/ManaDrillBlockEntity.java"
patch(
    mana_path,
    "import net.minecraft.world.level.block.state.BlockState;\n",
    "import net.minecraft.world.level.block.state.BlockState;\n"
    "import net.minecraft.world.phys.AABB;\n",
    "mana drill render bounds import",
)
patch(
    mana_path,
    "    @Override\n    public Component getDisplayName() {\n",
    "    @Override\n"
    "    public AABB getRenderBoundingBox() {\n"
    "        return new AABB(worldPosition).inflate(4.0D);\n"
    "    }\n\n"
    "    @Override\n    public Component getDisplayName() {\n",
    "mana drill enlarged render bounding box",
)
