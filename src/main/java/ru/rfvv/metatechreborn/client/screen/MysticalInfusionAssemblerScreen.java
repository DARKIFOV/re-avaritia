package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionAssemblerBlockEntity;
import ru.rfvv.metatechreborn.menu.MysticalInfusionAssemblerMenu;

public final class MysticalInfusionAssemblerScreen extends AbstractContainerScreen<MysticalInfusionAssemblerMenu> {
    public MysticalInfusionAssemblerScreen(MysticalInfusionAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 410; imageHeight = 260; inventoryLabelX = 116; inventoryLabelY = 162;
    }

    @Override public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); super.render(g, mouseX, mouseY, partialTick); renderMachineTooltip(g, mouseX, mouseY); renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 8, topPos + 18, 120, 132);
        MetaTechGui.panel(g, leftPos + 132, topPos + 18, 92, 132);
        MetaTechGui.panel(g, leftPos + 228, topPos + 8, 174, 142);
        MetaTechGui.panel(g, leftPos + 112, topPos + 168, 170, 86);

        int[] map = {4,0,1,2,3,5,6,7,8};
        for (int slot = 0; slot < 9; slot++) {
            int grid = map[slot];
            int accent = slot == 0 ? 0xFF8A57B8 : 0xFF57B881;
            MetaTechGui.slot(g, leftPos + 20 + (grid % 3) * 36,
                    topPos + 28 + (grid / 3) * 36, accent);
        }
        MetaTechGui.slot(g, leftPos + 148, topPos + 64, 0xFF57B881);
        MetaTechGui.slot(g, leftPos + 184, topPos + 64, 0xFFFFC857);

        for (int i = 0; i < 36; i++) {
            int accent = i < menu.getActivePatternSlots() ? 0xFF8A57B8 : 0xFF33444F;
            MetaTechGui.slot(g, leftPos + 236 + (i % 9) * 18, topPos + 28 + (i / 9) * 18, accent);
        }
        MetaTechGui.slot(g, leftPos + 236, topPos + 112, 0xFF8A57B8);
        for (int i = 0; i < 4; i++) MetaTechGui.slot(g, leftPos + 270 + i * 20, topPos + 112, 0xFF48BFE3);

        MetaTechGui.grid(g, leftPos + 116, topPos + 174, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 116, topPos + 232, 9, 1, 0xFF73879A);

        int progress = menu.getMaxProgress() <= 0 ? 0 : Math.min(74, menu.getProgress() * 74 / menu.getMaxProgress());
        g.fill(leftPos + 140, topPos + 104, leftPos + 216, topPos + 114, 0xFF03090D);
        g.fill(leftPos + 141, topPos + 105, leftPos + 141 + progress, topPos + 113, 0xFF57B881);
        int energy = menu.getEnergyCapacity() <= 0 ? 0 : Math.min(74, menu.getEnergy() * 74 / menu.getEnergyCapacity());
        g.fill(leftPos + 140, topPos + 120, leftPos + 216, topPos + 130, 0xFF03090D);
        g.fill(leftPos + 141, topPos + 121, leftPos + 141 + energy, topPos + 129, MetaTechGui.GOLD);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 6, 0xEAF8FF, false);
        g.drawString(font, Component.literal("Алтарь 1 + пьедесталы 8"), 16, 134, 0xCDB2EC, false);
        g.drawString(font, Component.literal("Результат"), 142, 50, 0xA9F0C7, false);
        g.drawString(font, Component.literal("FE"), 184, 50, 0xFFE09A, false);
        MetaTechGui.drawWrapped(g, font, statusText(), 140, 78, 76, statusColor(), 2);
        g.drawString(font, Component.literal("Шаблоны " + menu.getInstalledPatterns() + "/" + menu.getActivePatternSlots()), 236, 14, 0xD5B7F0, false);
        g.drawString(font, Component.literal("Банк+"), 234, 132, 0xD5B7F0, false);
        g.drawString(font, Component.literal("Ускорители ×" + menu.getSpeedCards()), 270, 132, 0x9CCBFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
    }

    private Component statusText() {
        return Component.literal(switch (menu.getStatus()) {
            case MysticalInfusionAssemblerBlockEntity.STATUS_NO_RECIPE -> "Нет Infusion-рецепта";
            case MysticalInfusionAssemblerBlockEntity.STATUS_NO_ENERGY -> "Недостаточно энергии";
            case MysticalInfusionAssemblerBlockEntity.STATUS_OUTPUT_FULL -> "Выход занят";
            case MysticalInfusionAssemblerBlockEntity.STATUS_RUNNING -> "Создание семени";
            case MysticalInfusionAssemblerBlockEntity.STATUS_AE2_READY -> "AE2: готов";
            default -> "Ожидание";
        });
    }
    private int statusColor() {
        return switch (menu.getStatus()) {
            case MysticalInfusionAssemblerBlockEntity.STATUS_RUNNING -> 0x78F0A2;
            case MysticalInfusionAssemblerBlockEntity.STATUS_AE2_READY -> 0x6ED7FF;
            case MysticalInfusionAssemblerBlockEntity.STATUS_NO_ENERGY -> 0xFFD56A;
            case MysticalInfusionAssemblerBlockEntity.STATUS_NO_RECIPE,
                 MysticalInfusionAssemblerBlockEntity.STATUS_OUTPUT_FULL -> 0xFF8A8A;
            default -> 0xBBD5E7;
        };
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 140, 104, 76, 10))
            g.renderTooltip(font, Component.literal("Прогресс: " + menu.getProgress() + " / " + menu.getMaxProgress()), mouseX, mouseY);
        else if (inside(mouseX, mouseY, 140, 120, 76, 10))
            g.renderTooltip(font, Component.literal("Энергия: " + menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"), mouseX, mouseY);
    }
    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }
}
