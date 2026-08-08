from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(dedent(content).lstrip(), encoding="utf-8")


write("src/main/java/ru/rfvv/metatechreborn/menu/LuckConverterMenu.java", r'''
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
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

public final class LuckConverterMenu extends AbstractContainerMenu {
    private final LuckConverterBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean advanced;
    private final int machineMenuSlots;

    public LuckConverterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                (LuckConverterBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(10));
    }

    public LuckConverterMenu(int id, Inventory inventory,
                             LuckConverterBlockEntity blockEntity, ContainerData data) {
        super(ModMenus.LUCK_CONVERTER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;
        this.advanced = blockEntity.isAdvanced();

        int inputColumns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        int inputY = 28;
        for (int row = 0; row < inputRows; row++) {
            for (int column = 0; column < inputColumns; column++) {
                int handlerSlot = column + row * inputColumns;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot,
                        10 + column * 18, inputY + row * 18));
            }
        }

        int outputY = advanced ? 148 : 96;
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                int handlerSlot = LuckConverterBlockEntity.FIRST_OUTPUT + column + row * 9;
                addSlot(new SlotItemHandler(blockEntity.getItems(), handlerSlot,
                        10 + column * 18, outputY + row * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                });
            }
        }

        int upgradesX = advanced ? 238 : 204;
        for (int index = 0; index < LuckConverterBlockEntity.UPGRADE_SLOTS; index++) {
            int column = index % 2;
            int row = index / 2;
            addSlot(new SlotItemHandler(blockEntity.getItems(),
                    LuckConverterBlockEntity.FIRST_UPGRADE + index,
                    upgradesX + column * 24, 40 + row * 24));
        }

        int utilityX = advanced ? 326 : 292;
        addSlot(new SlotItemHandler(blockEntity.getItems(),
                LuckConverterBlockEntity.MODULE_SLOT, utilityX, 40));
        addSlot(new SlotItemHandler(blockEntity.getItems(),
                LuckConverterBlockEntity.ENERGY_SLOT, utilityX, 72));
        this.machineMenuSlots = slots.size();

        int playerX = advanced ? 228 : 194;
        int playerY = advanced ? 222 : 188;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    playerX + column * 18, playerY + 58));
        }
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(
                        blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                advanced ? ModBlocks.ADVANCED_LUCK_CONVERTER.get()
                        : ModBlocks.LUCK_CONVERTER.get());
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

    public boolean isAdvanced() { return advanced; }
    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getEnergy() { return data.get(2); }
    public int getEnergyCapacity() { return data.get(3); }
    public int getLuckLevel() { return data.get(4); }
    public int getStatus() { return data.get(5); }
    public int getOperations() { return data.get(7); }
    public int getEnergyPerTick() { return data.get(8); }
    public int getSpeedBonusPercent() { return data.get(9); }
    public boolean isInstantSpeed() { return getSpeedBonusPercent() >= 100; }

    public int progressPixels(int width) {
        return getMaxProgress() <= 0 ? 0 : Math.min(width, getProgress() * width / getMaxProgress());
    }

    public int energyPixels(int width) {
        return getEnergyCapacity() <= 0 ? 0 : Math.min(width, getEnergy() * width / getEnergyCapacity());
    }
}
''')

write("src/main/java/ru/rfvv/metatechreborn/client/screen/LuckConverterScreen.java", r'''
package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.menu.LuckConverterMenu;

public final class LuckConverterScreen extends AbstractContainerScreen<LuckConverterMenu> {
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x606060;

    public LuckConverterScreen(LuckConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.isAdvanced() ? 398 : 364;
        imageHeight = menu.isAdvanced() ? 318 : 270;
        inventoryLabelX = menu.isAdvanced() ? 228 : 194;
        inventoryLabelY = menu.isAdvanced() ? 210 : 176;
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
        boolean advanced = menu.isAdvanced();
        int inputColumns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        int outputY = advanced ? 148 : 96;
        int sideX = advanced ? 230 : 196;
        int upgradesX = advanced ? 238 : 204;
        int utilityX = advanced ? 326 : 292;
        int playerX = advanced ? 228 : 194;
        int playerY = advanced ? 222 : 188;
        int sideWidth = 160;

        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20,
                inputColumns * 18 + 8, inputRows * 18 + 14);
        MetaTechGui.panel(g, leftPos + 6, topPos + outputY - 6, 170, 170);
        MetaTechGui.panel(g, leftPos + sideX, topPos + 20, sideWidth, 148);
        MetaTechGui.panel(g, leftPos + playerX - 4, topPos + playerY - 8, 170, 88);

        MetaTechGui.grid(g, leftPos + 10, topPos + 28, inputColumns, inputRows, 0xFF3A86B8);
        MetaTechGui.grid(g, leftPos + 10, topPos + outputY, 9, 9, 0xFF7653A6);

        for (int index = 0; index < LuckConverterBlockEntity.UPGRADE_SLOTS; index++) {
            int column = index % 2;
            int row = index / 2;
            int accent = index < 3 ? 0xFF3A86B8 : 0xFFD89B2B;
            MetaTechGui.slot(g, leftPos + upgradesX + column * 24,
                    topPos + 40 + row * 24, accent);
        }
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 40, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 72, 0xFFD89B2B);

        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY + 58, 9, 1, 0xFF777777);

        int barX = sideX + 8;
        int barW = sideWidth - 16;
        drawBar(g, barX, 110, barW, menu.progressPixels(barW - 2), MetaTechGui.CYAN);
        drawBar(g, barX, 128, barW, menu.energyPixels(barW - 2), MetaTechGui.GOLD);
        int speed = menu.isInstantSpeed() ? barW - 2
                : Math.min(barW - 2, (barW - 2) * menu.getSpeedBonusPercent() / 100);
        drawBar(g, barX, 92, barW, speed, menu.isInstantSpeed() ? 0xFF8E44AD : 0xFF3A9D72);
    }

    private void drawBar(GuiGraphics g, int x, int y, int width, int fill, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 8, 0xFF555555);
        if (fill > 0) g.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + 1 + fill, topPos + y + 7, color);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int sideX = advanced ? 230 : 196;
        int playerX = advanced ? 228 : 194;

        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.upgrades"),
                sideX + 8, 26, TEXT, false);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable(statusKey(menu.getStatus())),
                sideX + 8, 70, 144, statusColor(menu.getStatus()), 2);
        g.drawString(font, Component.literal("×" + menu.getOperations() + "  L" + menu.getLuckLevel()),
                sideX + 8, 146, 0x255C88, false);
        g.drawString(font, playerInventoryTitle,
                playerX, inventoryLabelY, MUTED, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int sideX = advanced ? 230 : 196;
        int utilityX = advanced ? 326 : 292;
        int upgradesX = advanced ? 238 : 204;
        int barW = 144;

        if (isInside(mouseX, mouseY, sideX + 8, 110, barW, 8)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, sideX + 8, 128, barW, 8)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy",
                    menu.getEnergy(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, sideX + 8, 92, barW, 8)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.speed", speedText()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, utilityX, 40, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.module"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, utilityX, 72, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy_slot"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, upgradesX, 40, 42, 66)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.upgrades"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private Component speedText() {
        if (menu.isInstantSpeed()) return Component.translatable("gui.metatech_reborn.luck_converter.speed.instant");
        if (menu.getSpeedBonusPercent() <= 0) return Component.translatable("gui.metatech_reborn.luck_converter.speed.none");
        return Component.translatable("gui.metatech_reborn.luck_converter.speed.percent", menu.getSpeedBonusPercent());
    }

    private static String statusKey(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.luck_converter.status.running";
            case LuckConverterBlockEntity.STATUS_NO_MODULE -> "gui.metatech_reborn.luck_converter.status.no_module";
            case LuckConverterBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.luck_converter.status.no_energy";
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.luck_converter.status.output_full";
            case LuckConverterBlockEntity.STATUS_NO_VALID_INPUT -> "gui.metatech_reborn.luck_converter.status.no_input";
            default -> "gui.metatech_reborn.luck_converter.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING -> 0x176B45;
            case LuckConverterBlockEntity.STATUS_NO_ENERGY -> 0x8A6200;
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL -> 0xA04400;
            case LuckConverterBlockEntity.STATUS_NO_MODULE,
                 LuckConverterBlockEntity.STATUS_NO_VALID_INPUT -> 0xA02020;
            default -> MUTED;
        };
    }
}
''')

# The custom formed drill renderer must be no-cull. entitySolid caused large black
# triangles/planes when the camera was close to the assembled machine.
renderer = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/renderer/ManaDrillRenderer.java"
text = renderer.read_text(encoding="utf-8")
text = text.replace("RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS)",
                    "RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)")
renderer.write_text(text, encoding="utf-8")

# Keep the existing Mana Drill layout but abbreviate the large mana value so it
# cannot run outside the left status panel.
mana_screen = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/screen/ManaDrillScreen.java"
text = mana_screen.read_text(encoding="utf-8")
text = text.replace('menu.getMana(), menu.getManaCapacity()),',
                    'compact(menu.getMana()), compact(menu.getManaCapacity())),')
if "private static String compact(int value)" not in text:
    marker = "    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {"
    helper = '''    private static String compact(int value) {\n        long v = Integer.toUnsignedLong(value);\n        if (v >= 1_000_000_000L) return String.format(java.util.Locale.ROOT, \"%.1fG\", v / 1_000_000_000.0);\n        if (v >= 1_000_000L) return String.format(java.util.Locale.ROOT, \"%.1fM\", v / 1_000_000.0);\n        if (v >= 1_000L) return String.format(java.util.Locale.ROOT, \"%.1fK\", v / 1_000.0);\n        return Long.toString(v);\n    }\n\n'''
    text = text.replace(marker, helper + marker)
mana_screen.write_text(text, encoding="utf-8")
