package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;

public final class NeutroniumCombinerScreen extends AbstractContainerScreen<NeutroniumCombinerMenu> {
    public NeutroniumCombinerScreen(NeutroniumCombinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 220;
        inventoryLabelX = 50;
        inventoryLabelY = 116;
    }
    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20252B);
        graphics.fill(leftPos + 4, topPos + 14, leftPos + 98, topPos + 112, 0xFF343B44);
        graphics.fill(leftPos + 100, topPos + 14, leftPos + 252, topPos + 112, 0xFF343B44);
        graphics.fill(leftPos + 46, topPos + 124, leftPos + 214, topPos + 216, 0xFF343B44);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            int x = leftPos + 7 + column * 28;
            int y = topPos + 19 + row * 28;
            drawSlot(graphics, x, y);
            graphics.fill(x, y + 19, x + 18, y + 22, 0xFF101418);
            int pixels = menu.getProgressPixels(column + row * 3, 16);
            graphics.fill(x + 1, y + 20, x + 1 + pixels, y + 21, 0xFF60A5FA);
        }
        for (int row = 0; row < 5; row++) for (int column = 0; column < 8; column++) {
            drawSlot(graphics, leftPos + 103 + column * 18, topPos + 17 + row * 18);
        }
        graphics.fill(leftPos + 88, topPos + 20, leftPos + 96, topPos + 105, 0xFF101418);
        int energyPixels = menu.getEnergyPixels(83);
        graphics.fill(leftPos + 89, topPos + 104 - energyPixels, leftPos + 95, topPos + 105, 0xFFFFC857);
    }
    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF111418);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8A949E);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF2A3038);
    }
    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal(menu.getEnergyStored() + " / " + menu.getEnergyCapacity() + " FE"),
                8, 105, 0xFFFFFF, false);
    }
}
