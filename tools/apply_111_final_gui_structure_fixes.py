from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(dedent(content).lstrip(), encoding="utf-8")


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
# Neutronium Combiner
# Keep the fourth historical storage index only for save compatibility, but make
# only three upgrade slots active/visible because there are only three upgrade types.
# ---------------------------------------------------------------------------
patch(
    "src/main/java/ru/rfvv/metatechreborn/blockentity/NeutroniumCombinerBlockEntity.java",
    "    public static final int UPGRADE_SLOTS = 4;\n",
    "    public static final int UPGRADE_SLOTS = 4; // historical storage width for old worlds\n"
    "    public static final int ACTIVE_UPGRADE_SLOTS = 3;\n",
    "neutron active upgrade slot count",
)
patch(
    "src/main/java/ru/rfvv/metatechreborn/blockentity/NeutroniumCombinerBlockEntity.java",
    "            if (slot >= FIRST_UPGRADE_SLOT && slot < ENERGY_SLOT) {\n"
    "                return stack.getItem() instanceof NeutroniumCombinerUpgradeItem;\n"
    "            }\n",
    "            if (slot >= FIRST_UPGRADE_SLOT && slot < FIRST_UPGRADE_SLOT + ACTIVE_UPGRADE_SLOTS) {\n"
    "                return stack.getItem() instanceof NeutroniumCombinerUpgradeItem;\n"
    "            }\n",
    "neutron reject hidden legacy upgrade slot",
)
patch(
    "src/main/java/ru/rfvv/metatechreborn/blockentity/NeutroniumCombinerBlockEntity.java",
    "        for (int slot = FIRST_UPGRADE_SLOT; slot < ENERGY_SLOT; slot++) {\n",
    "        for (int slot = FIRST_UPGRADE_SLOT; slot < FIRST_UPGRADE_SLOT + ACTIVE_UPGRADE_SLOTS; slot++) {\n",
    "neutron count only three upgrades",
)

write("src/main/java/ru/rfvv/metatechreborn/menu/NeutroniumCombinerMenu.java", r'''
package ru.rfvv.metatechreborn.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class NeutroniumCombinerMenu extends AbstractContainerMenu {
    private final NeutroniumCombinerBlockEntity blockEntity;
    private final ContainerData data;
    private final int machineMenuSlots;

    public NeutroniumCombinerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory,
                (NeutroniumCombinerBlockEntity) playerInventory.player.level()
                        .getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(32));
    }

    public NeutroniumCombinerMenu(int containerId, Inventory playerInventory,
                                  NeutroniumCombinerBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.NEUTRONIUM_COMBINER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        12 + column * 30, 30 + row * 30));
            }
        }

        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 8; column++) {
                int slot = NeutroniumCombinerBlockEntity.FIRST_OUTPUT_SLOT + column + row * 8;
                addSlot(new SlotItemHandler(blockEntity.getItems(), slot,
                        126 + column * 18, 28 + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        // Exactly three upgrade slots: speed, efficiency, output.
        for (int column = 0; column < NeutroniumCombinerBlockEntity.ACTIVE_UPGRADE_SLOTS; column++) {
            int handlerSlot = NeutroniumCombinerBlockEntity.FIRST_UPGRADE_SLOT + column;
            addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot, 12 + column * 24, 130));
        }
        // Energy item is visually separated from the three upgrade slots.
        addSlot(new SlotItemHandler(blockEntity.getItems(),
                NeutroniumCombinerBlockEntity.ENERGY_SLOT, 90, 130));
        this.machineMenuSlots = slots.size();

        int inventoryX = 91;
        int inventoryY = 196;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        inventoryX + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    inventoryX + column * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.NEUTRONIUM_COMBINER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index < machineMenuSlots) {
            if (!moveItemStackTo(original, machineMenuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, machineMenuSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, original);
        return copy;
    }

    public int getProgress(int inputSlot) { return data.get(inputSlot); }
    public int getMaxProgress(int inputSlot) {
        return data.get(NeutroniumCombinerBlockEntity.INPUT_SLOTS + inputSlot);
    }
    public int getProgressPixels(int inputSlot, int width) {
        int maximum = getMaxProgress(inputSlot);
        return maximum <= 0 ? 0 : Math.min(width, getProgress(inputSlot) * width / maximum);
    }
    public int getStatus(int inputSlot) {
        return data.get(NeutroniumCombinerBlockEntity.INPUT_SLOTS * 2 + inputSlot);
    }
    public int getEnergyStored() { return data.get(27); }
    public int getEnergyCapacity() { return data.get(28); }
    public int getSpeedUpgrades() { return data.get(29); }
    public int getEfficiencyUpgrades() { return data.get(30); }
    public int getOutputUpgrades() { return data.get(31); }
    public int getEnergyPixels(int width) {
        int capacity = getEnergyCapacity();
        return capacity <= 0 ? 0 : Math.min(width, getEnergyStored() * width / capacity);
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/client/screen/NeutroniumCombinerScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;

public final class NeutroniumCombinerScreen extends AbstractContainerScreen<NeutroniumCombinerMenu> {
    public NeutroniumCombinerScreen(NeutroniumCombinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 344;
        imageHeight = 282;
        inventoryLabelX = 91;
        inventoryLabelY = 184;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 104);
        MetaTechGui.panel(g, leftPos + 120, topPos + 20, 218, 104);
        MetaTechGui.panel(g, leftPos + 6, topPos + 126, 112, 48);
        MetaTechGui.panel(g, leftPos + 120, topPos + 126, 218, 48);
        MetaTechGui.panel(g, leftPos + 87, topPos + 190, 170, 86);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int input = column + row * 3;
                int x = leftPos + 12 + column * 30;
                int y = topPos + 30 + row * 30;
                MetaTechGui.slot(g, x - 1, y - 1, 0xFF48BFE3);
                int pixels = menu.getProgressPixels(input, 16);
                g.fill(x, y + 18, x + 16, y + 21, 0xFF03090D);
                g.fill(x, y + 18, x + pixels, y + 21, MetaTechGui.CYAN);
                g.fill(x + 18, y, x + 22, y + 4, statusColor(menu.getStatus(input)));
            }
        }

        MetaTechGui.grid(g, leftPos + 125, topPos + 27, 8, 5, 0xFF73879A);

        // Three upgrade slots plus one separate energy-item slot.
        for (int column = 0; column < NeutroniumCombinerBlockEntity.ACTIVE_UPGRADE_SLOTS; column++) {
            MetaTechGui.slot(g, leftPos + 11 + column * 24, topPos + 129, 0xFFFFA43A);
        }
        MetaTechGui.slot(g, leftPos + 89, topPos + 129, 0xFFFFC857);

        MetaTechGui.grid(g, leftPos + 90, topPos + 195, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 90, topPos + 253, 9, 1, 0xFF73879A);

        // Horizontal energy bar aligned inside the lower-right panel.
        int energyWidth = 202;
        int energyPixels = menu.getEnergyPixels(energyWidth - 2);
        int barX = leftPos + 128;
        int barY = topPos + 154;
        g.fill(barX, barY, barX + energyWidth, barY + 10, 0xFF03090D);
        if (energyPixels > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + energyPixels, barY + 9, MetaTechGui.GOLD);
        }
    }

    private static int statusColor(int status) {
        return switch (status) {
            case NeutroniumCombinerBlockEntity.STATUS_RUNNING -> 0xFF4EE08A;
            case NeutroniumCombinerBlockEntity.STATUS_NO_RECIPE -> 0xFFFF4F67;
            case NeutroniumCombinerBlockEntity.STATUS_NO_ENERGY -> 0xFFFFC857;
            case NeutroniumCombinerBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF8B45;
            default -> 0xFF53666D;
        };
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.collectors"),
                10, 22, 0x9CCBFF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.outputs"),
                124, 22, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.upgrades"),
                10, 128, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.energy",
                        menu.getEnergyStored(), menu.getEnergyCapacity()),
                128, 136, 0xF4D27A, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int input = column + row * 3;
                int x = 12 + column * 30;
                int y = 30 + row * 30;
                if (isInside(mouseX, mouseY, x, y, 22, 22)) {
                    g.renderTooltip(font, Component.translatable(
                            "gui.metatech_reborn.neutron.tooltip.process",
                            input + 1, menu.getProgress(input), menu.getMaxProgress(input),
                            Component.translatable(statusKey(menu.getStatus(input)))),
                            mouseX, mouseY);
                    return;
                }
            }
        }
        if (isInside(mouseX, mouseY, 128, 154, 202, 10)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy",
                    menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 12, 130, 66, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.neutron.tooltip.upgrades",
                    menu.getSpeedUpgrades(), menu.getEfficiencyUpgrades(), menu.getOutputUpgrades()),
                    mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static String statusKey(int status) {
        return switch (status) {
            case NeutroniumCombinerBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.neutron.status.running";
            case NeutroniumCombinerBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.neutron.status.recipe";
            case NeutroniumCombinerBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.neutron.status.energy";
            case NeutroniumCombinerBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.neutron.status.output";
            default -> "gui.metatech_reborn.neutron.status.idle";
        };
    }
}
''')

# ---------------------------------------------------------------------------
# Dragon GUIs: the visual frame is 18x18, while Minecraft renders a 16x16 item
# at Slot.x/y. The old menu used frame+2; use frame+1 so item is centered.
# ---------------------------------------------------------------------------
encoder_menu = ROOT / "src/main/java/ru/rfvv/metatechreborn/menu/DragonPatternEncoderMenu.java"
text = encoder_menu.read_text(encoding="utf-8")
replacements = {
    "204, 54": "203, 53",
    "260, 54": "259, 53",
    "16, 58": "15, 57",
    "60 + (i % 4) * 20, 38 + (i / 4) * 20": "59 + (i % 4) * 20, 37 + (i / 4) * 20",
    "158, 58": "157, 57",
    "int inventoryX = 72;": "int inventoryX = 71;",
    "int inventoryY = 158;": "int inventoryY = 157;",
}
for old, new in replacements.items():
    if old not in text and new not in text:
        raise RuntimeError(f"dragon encoder coordinate not found: {old}")
    text = text.replace(old, new)
encoder_menu.write_text(text, encoding="utf-8")

assembler_menu = ROOT / "src/main/java/ru/rfvv/metatechreborn/menu/ExtremeDragonAssemblerMenu.java"
text = assembler_menu.read_text(encoding="utf-8")
replacements = {
    "12 + (i % 2) * 20, 36 + (i / 2) * 20": "11 + (i % 2) * 20, 35 + (i / 2) * 20",
    "68 + (i % 4) * 20, 40 + (i / 4) * 20": "67 + (i % 4) * 20, 39 + (i / 4) * 20",
    "158, 60": "157, 59",
    "190, 60": "189, 59",
    "224 + (i % 9) * 18, 36 + (i / 9) * 18": "223 + (i % 9) * 18, 35 + (i / 9) * 18",
    "int inventoryX = 114;": "int inventoryX = 113;",
    "int inventoryY = 180;": "int inventoryY = 179;",
}
for old, new in replacements.items():
    if old not in text and new not in text:
        raise RuntimeError(f"dragon assembler coordinate not found: {old}")
    text = text.replace(old, new)
assembler_menu.write_text(text, encoding="utf-8")

# Preserve the 0.6.110 energy fix even though the dragon source is generated by
# an earlier hotfix script during every clean build.
assembler_be = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"
text = assembler_be.read_text(encoding="utf-8")
old = "new EnergyStorage(2_000_000_000, 100_000_000, 0)"
new = "new EnergyStorage(2_000_000_000, 100_000_000, 100_000_000)"
if old in text:
    text = text.replace(old, new)
elif new not in text:
    raise RuntimeError("dragon assembler EnergyStorage constructor not found")
assembler_be.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# Mana Drill: remove the mirrored/legacy assembly direction that can make the
# renderer appear on the wrong side of the physical 3x3x3 structure. Also sync
# visual state on every structure check so invalid structures immediately return
# to normal visible blocks instead of leaving a ghost assembled shell.
# ---------------------------------------------------------------------------
structure = ROOT / "src/main/java/ru/rfvv/metatechreborn/multiblock/ManaDrillStructure.java"
text = structure.read_text(encoding="utf-8")
old = '''        if (isFormedInDirection(level, controller, right, normalDepth)) {
            return Optional.of(new Match(normalDepth, false));
        }
        if (isFormedInDirection(level, controller, right, facing)) {
            return Optional.of(new Match(facing, true));
        }
        return Optional.empty();
'''
new = '''        if (isFormedInDirection(level, controller, right, normalDepth)) {
            return Optional.of(new Match(normalDepth, false));
        }
        // Mirrored legacy assembly is intentionally no longer accepted: it can
        // place the one-piece render on the opposite side of the real structure.
        return Optional.empty();
'''
if old in text:
    text = text.replace(old, new)
elif new not in text:
    raise RuntimeError("ManaDrillStructure legacy-direction block not found")
structure.write_text(text, encoding="utf-8")

mana_be = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ManaDrillBlockEntity.java"
text = mana_be.read_text(encoding="utf-8")
old = '''        if (formed != structureFormed) {
            structureFormed = formed;
            if (!formed) resetProgress();
            setChanged();
        }
'''
new = '''        if (formed != structureFormed) {
            structureFormed = formed;
            if (!formed) resetProgress();
            setChanged();
        }
        // Always reconcile the physical blocks with the current validation result.
        // This clears stale FORMED states after any broken/changed structure.
        ManaDrillStructure.syncVisualState(level, worldPosition, facing, formed);
'''
if old in text:
    text = text.replace(old, new)
elif new not in text:
    raise RuntimeError("ManaDrillBlockEntity structure-sync block not found")
mana_be.write_text(text, encoding="utf-8")
