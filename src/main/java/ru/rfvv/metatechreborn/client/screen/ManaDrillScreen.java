package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    private static final int TEXT = 0x404040;
    private static final int MANA = 0xFF3A86B8;
    private static final int PROGRESS = 0xFF7653A6;
    private static final int TRACK = 0xFF3D4147;

    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 324;
        imageHeight = 324;
        inventoryLabelX = 81;
        inventoryLabelY = 228;
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
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 196);
        MetaTechGui.panel(g, leftPos + 124, topPos + 20, 194, 176);
        MetaTechGui.panel(g, leftPos + 77, topPos + 234, 170, 84);

        MetaTechGui.slot(g, leftPos + 20, topPos + 34, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + 20, topPos + 64, MANA);
        MetaTechGui.slot(g, leftPos + 44, topPos + 64, PROGRESS);
        MetaTechGui.slot(g, leftPos + 68, topPos + 64, 0xFFD89B2B);
        MetaTechGui.grid(g, leftPos + 132, topPos + 30, 9, 9, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 240, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 298, 9, 1, 0xFF777777);

        drawHorizontalBar(g, 12, 108, 98, 12, menu.getManaPixels(94), MANA);
        drawHorizontalBar(g, 12, 140, 98, 12, menu.getProgressPixels(94), PROGRESS);
    }

    private void drawHorizontalBar(GuiGraphics g, int x, int y, int width, int height,
                                   int filled, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF202329);
        g.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + width - 1, topPos + y + height - 1, TRACK);
        int clamped = Math.max(0, Math.min(width - 4, filled));
        if (clamped > 0) {
            g.fill(leftPos + x + 2, topPos + y + 2,
                    leftPos + x + 2 + clamped, topPos + y + height - 2, color);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, TEXT, false);

        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.mana",
                        menu.getMana(), menu.getManaCapacity()),
                12, 96, 0x255C88, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.tooltip.progress_ticks",
                        menu.getProgress(), menu.getMaxProgress()),
                12, 128, 0x5A3D82, false);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable(menu.isStructureFormed()
                        ? "gui.metatech_reborn.structure_formed"
                        : "gui.metatech_reborn.structure_missing"),
                12, 158, 98, menu.isStructureFormed() ? 0x176B45 : 0xA02020, 2);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 12, 108, 98, 12)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 12, 140, 98, 12)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
