package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;

public final class MolecularAssemblerScreen extends AbstractContainerScreen<MolecularAssemblerMenu> {
    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256; imageHeight = 270; inventoryLabelY = 176;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.unlock_recipe"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MolecularAssemblerMenu.UNLOCK_BUTTON_ID);
        }).bounds(leftPos + 174, topPos + 18, 74, 20).build());
    }
    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20252B);
        graphics.fill(leftPos + 4, topPos + 14, leftPos + 170, topPos + 180, 0xFF343B44);
        graphics.fill(leftPos + 172, topPos + 14, leftPos + 252, topPos + 180, 0xFF343B44);
        graphics.fill(leftPos + 4, topPos + 184, leftPos + 170, topPos + 266, 0xFF343B44);
        for (int row = 0; row < 9; row++) for (int column = 0; column < 9; column++)
            drawSlot(graphics, leftPos + 7 + column * 18, topPos + 17 + row * 18);
        drawSlot(graphics, leftPos + 189, topPos + 81);
        graphics.fill(leftPos + 176, topPos + 111, leftPos + 244, topPos + 121, 0xFF101418);
        graphics.fill(leftPos + 177, topPos + 112, leftPos + 177 + menu.getProgressPixels(66), topPos + 120, 0xFF60A5FA);
        graphics.fill(leftPos + 235, topPos + 48, leftPos + 245, topPos + 101, 0xFF101418);
        int energy = menu.getEnergyPixels(51);
        graphics.fill(leftPos + 236, topPos + 100 - energy, leftPos + 244, topPos + 101, 0xFFFFC857);
    }
    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF111418);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8A949E);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF2A3038);
    }
    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable(menu.isRecipeLocked()
                ? "gui.metatech_reborn.recipe_locked" : "gui.metatech_reborn.recipe_unlocked"),
                176, 42, menu.isRecipeLocked() ? 0x86EFAC : 0xFCA5A5, false);
        graphics.drawString(font, Component.literal(menu.getEnergyStored() + " / " + menu.getEnergyCapacity() + " FE"),
                176, 128, 0xFFFFFF, false);
    }
}
