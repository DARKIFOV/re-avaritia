package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.menu.LuckConverterMenu;

public final class LuckConverterScreen extends AbstractContainerScreen<LuckConverterMenu> {
    public LuckConverterScreen(LuckConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.isAdvanced() ? 302 : 266;
        imageHeight = menu.isAdvanced() ? 400 : 302;
        inventoryLabelX = menu.isAdvanced() ? 70 : 52;
        inventoryLabelY = menu.isAdvanced() ? 306 : 206;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        MetaTechGui.background(g, x, y, imageWidth, imageHeight);

        int columns = menu.isAdvanced() ? 12 : 10;
        int inputRows = menu.isAdvanced() ? 6 : 3;
        int outputRows = menu.isAdvanced() ? 5 : 3;
        int outputY = menu.isAdvanced() ? 146 : 92;
        int statusY = menu.isAdvanced() ? 244 : 154;
        int playerX = menu.isAdvanced() ? 70 : 52;
        int playerY = menu.isAdvanced() ? 318 : 218;
        int sideX = menu.isAdvanced() ? 270 : 214;
        int gridWidth = columns * 18;

        MetaTechGui.panel(g, x + 6, y + 20, gridWidth + 8, inputRows * 18 + 12);
        MetaTechGui.panel(g, x + 6, y + outputY - 8, gridWidth + 8, outputRows * 18 + 12);
        MetaTechGui.panel(g, x + 6, y + statusY, gridWidth + 8, 50);
        MetaTechGui.panel(g, x + sideX - 8, y + 20, 34, 184);
        MetaTechGui.panel(g, x + playerX - 4, y + playerY - 6, 170, 86);

        MetaTechGui.grid(g, x + 10, y + 28, columns, inputRows, 0xFF42CAE8);
        MetaTechGui.grid(g, x + 10, y + outputY, columns, outputRows, 0xFF7356D8);
        for (int i = 0; i < 6; i++) MetaTechGui.slot(g, x + sideX, y + 28 + i * 20, 0xFFFFA43A);
        MetaTechGui.slot(g, x + sideX, y + 156, 0xFF55E58A);
        MetaTechGui.slot(g, x + sideX, y + 180, 0xFFFFCC45);
        MetaTechGui.grid(g, x + playerX, y + playerY, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, x + playerX, y + playerY + 58, 9, 1, 0xFF73879A);

        int barX = x + 10;
        int barY = y + statusY + 37;
        g.fill(barX, barY, barX + gridWidth, barY + 8, 0xFF03090D);
        g.fill(barX + 1, barY + 1, barX + 1 + menu.progressPixels(gridWidth - 2), barY + 7, MetaTechGui.CYAN);

        int energyHeight = 80;
        int energyPixels = menu.energyPixels(energyHeight);
        g.fill(x + sideX + 22, y + 28, x + sideX + 28, y + 28 + energyHeight, 0xFF03090D);
        g.fill(x + sideX + 23, y + 28 + energyHeight - energyPixels,
                x + sideX + 27, y + 28 + energyHeight, MetaTechGui.GOLD);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        int columns = menu.isAdvanced() ? 12 : 10;
        int outputY = menu.isAdvanced() ? 146 : 92;
        int statusY = menu.isAdvanced() ? 244 : 154;
        int gridWidth = columns * 18;

        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.inputs"),
                10, 20, 0x9CCBFF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.outputs"),
                10, outputY - 8, 0xC0A8FF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);

        MetaTechGui.drawWrapped(g, font, Component.translatable(statusKey(menu.getStatus())),
                10, statusY + 5, gridWidth - 8, statusColor(menu.getStatus()), 2);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.stats",
                        menu.getLuckLevel(), menu.getOperations(), menu.getEnergyPerTick()),
                10, statusY + 23, 0x9CD8FF, false);
        g.drawString(font, Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"),
                10, statusY + 33, 0xF4D27A, false);
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
            case LuckConverterBlockEntity.STATUS_RUNNING -> 0x55E58A;
            case LuckConverterBlockEntity.STATUS_NO_ENERGY -> 0xF5CA59;
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL -> 0xFF9454;
            case LuckConverterBlockEntity.STATUS_NO_MODULE, LuckConverterBlockEntity.STATUS_NO_VALID_INPUT -> 0xFF6477;
            default -> 0xA9BAC5;
        };
    }
}
