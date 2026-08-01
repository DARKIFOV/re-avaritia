package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 272;
        imageHeight = 184;
        inventoryLabelX = 50;
        inventoryLabelY = 80;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171D27);
        graphics.fill(leftPos + 6, topPos + 14, leftPos + 86, topPos + 76, 0xFF273244);
        graphics.fill(leftPos + 88, topPos + 14, leftPos + 268, topPos + 76, 0xFF273244);
        graphics.fill(leftPos + 46, topPos + 88, leftPos + 214, topPos + 180, 0xFF273244);
        drawSlot(graphics, leftPos + 17, topPos + 25);
        drawSlot(graphics, leftPos + 17, topPos + 49);
        drawSlot(graphics, leftPos + 41, topPos + 49);
        drawSlot(graphics, leftPos + 65, topPos + 49);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 91 + column * 18, topPos + 17 + row * 18);
        }
        graphics.fill(leftPos + 10, topPos + 16, leftPos + 15, topPos + 72, 0xFF0E141D);
        int manaPixels = menu.getManaPixels(54);
        graphics.fill(leftPos + 11, topPos + 71 - manaPixels, leftPos + 14, topPos + 72, 0xFF42D7F5);
        graphics.fill(leftPos + 18, topPos + 70, leftPos + 83, topPos + 74, 0xFF0E141D);
        int progressPixels = menu.getProgressPixels(63);
        graphics.fill(leftPos + 19, topPos + 71, leftPos + 19 + progressPixels, topPos + 73, 0xFFB26CFF);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF0E1117);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF7F8FA6);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF202A38);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal(menu.getMana() + " / " + menu.getManaCapacity() + " Mana"),
                90, 6, 0x6EE7F9, false);
    }
}
