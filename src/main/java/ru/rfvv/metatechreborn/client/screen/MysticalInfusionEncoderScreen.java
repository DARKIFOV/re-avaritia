package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.MysticalInfusionEncoderMenu;

public final class MysticalInfusionEncoderScreen extends AbstractContainerScreen<MysticalInfusionEncoderMenu> {
    public MysticalInfusionEncoderScreen(MysticalInfusionEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300; imageHeight = 226; inventoryLabelX = 78; inventoryLabelY = 130;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Кодировать"), b -> send(MysticalInfusionEncoderMenu.ENCODE_BUTTON))
                .bounds(leftPos + 182, topPos + 78, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Очистить"), b -> send(MysticalInfusionEncoderMenu.CLEAR_BUTTON))
                .bounds(leftPos + 182, topPos + 102, 106, 20).build());
    }
    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); super.render(g, mouseX, mouseY, partialTick); renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 8, topPos + 18, 164, 116);
        MetaTechGui.panel(g, leftPos + 176, topPos + 18, 116, 116);
        MetaTechGui.panel(g, leftPos + 74, topPos + 136, 170, 84);

        int[] map = {0,1,2,3,5,6,7,8};
        MetaTechGui.slot(g, leftPos + 78, topPos + 56, 0xFF8A57B8);
        for (int i = 0; i < 8; i++) {
            int grid = map[i];
            MetaTechGui.slot(g, leftPos + 42 + (grid % 3) * 36,
                    topPos + 20 + (grid / 3) * 36, 0xFF57B881);
        }
        MetaTechGui.slot(g, leftPos + 148, topPos + 56, 0xFFB88757);
        MetaTechGui.slot(g, leftPos + 184, topPos + 38, 0xFF8A57B8);
        MetaTechGui.slot(g, leftPos + 256, topPos + 38, 0xFF57B881);
        MetaTechGui.grid(g, leftPos + 78, topPos + 142, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 78, topPos + 200, 9, 1, 0xFF73879A);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 6, 0xEAF8FF, false);
        g.drawString(font, Component.literal("Алтарь"), 68, 104, 0xD5B7F0, false);
        g.drawString(font, Component.literal("Пустой"), 180, 26, 0xD5B7F0, false);
        g.drawString(font, Component.literal("Шаблон"), 248, 26, 0xA9F0C7, false);
        MetaTechGui.drawWrapped(g, font, statusText(), 182, 52, 104, statusColor(), 2);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
    }

    private Component statusText() {
        if (!menu.hasRecipe()) return Component.literal("Выбери Infusion-рецепт в JEI");
        return switch (menu.getStatus()) {
            case MysticalInfusionEncoderBlockEntity.STATUS_READY -> Component.literal("Рецепт загружен");
            case MysticalInfusionEncoderBlockEntity.STATUS_ENCODED -> Component.literal("Шаблон закодирован");
            case MysticalInfusionEncoderBlockEntity.STATUS_NO_BLANK -> Component.literal("Нужен пустой шаблон");
            case MysticalInfusionEncoderBlockEntity.STATUS_OUTPUT_BLOCKED -> Component.literal("Выход занят");
            case MysticalInfusionEncoderBlockEntity.STATUS_NO_RECIPE -> Component.literal("Рецепт не выбран");
            default -> Component.literal("Ожидание");
        };
    }
    private int statusColor() {
        return switch (menu.getStatus()) {
            case MysticalInfusionEncoderBlockEntity.STATUS_READY,
                 MysticalInfusionEncoderBlockEntity.STATUS_ENCODED -> 0x78F0A2;
            case MysticalInfusionEncoderBlockEntity.STATUS_NO_BLANK,
                 MysticalInfusionEncoderBlockEntity.STATUS_OUTPUT_BLOCKED,
                 MysticalInfusionEncoderBlockEntity.STATUS_NO_RECIPE -> 0xFF8A8A;
            default -> 0x9CCBFF;
        };
    }
}
