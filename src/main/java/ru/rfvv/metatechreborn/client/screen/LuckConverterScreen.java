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
        imageWidth = menu.isAdvanced() ? 262 : 230;
        imageHeight = menu.isAdvanced() ? 336 : 258;
        inventoryLabelX = menu.isAdvanced() ? 37 : 28;
        inventoryLabelY = menu.isAdvanced() ? 243 : 165;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF07151C);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF14232F);
        g.fill(x + 6, y + 6, x + imageWidth - 6, y + imageHeight - 6, 0xFF101925);

        int columns = menu.isAdvanced() ? 12 : 10;
        int inputRows = menu.isAdvanced() ? 6 : 3;
        int outputRows = menu.isAdvanced() ? 5 : 3;
        drawSlotGrid(g, x + 9, y + 19, columns, inputRows, 0xFF42CAE8);
        drawSlotGrid(g, x + 9, y + (menu.isAdvanced() ? 133 : 83), columns, outputRows, 0xFF7356D8);

        int sideX = x + (menu.isAdvanced() ? 237 : 193);
        for (int i = 0; i < 6; i++) drawSlot(g, sideX, y + 7 + i * 18, 0xFFFFA43A);
        drawSlot(g, sideX, y + 115, 0xFF55E58A);
        drawSlot(g, sideX, y + (menu.isAdvanced() ? 153 : 139), 0xFFFFCC45);

        int barX = x + 10;
        int barY = y + (menu.isAdvanced() ? 228 : 150);
        int barWidth = menu.isAdvanced() ? 216 : 180;
        g.fill(barX, barY, barX + barWidth, barY + 8, 0xFF050A0D);
        g.fill(barX + 1, barY + 1, barX + 1 + menu.progressPixels(barWidth - 2), barY + 7, 0xFF42D7F5);

        int energyHeight = menu.isAdvanced() ? 64 : 44;
        int energyPixels = menu.energyPixels(energyHeight);
        g.fill(sideX + 24, y + 8, sideX + 30, y + 8 + energyHeight, 0xFF050A0D);
        g.fill(sideX + 25, y + 8 + energyHeight - energyPixels, sideX + 29, y + 8 + energyHeight, 0xFFFFC857);
    }

    private static void drawSlotGrid(GuiGraphics g, int x, int y, int columns, int rows, int accent) {
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) drawSlot(g, x + col * 18, y + row * 18, accent);
    }

    private static void drawSlot(GuiGraphics g, int x, int y, int accent) {
        g.fill(x, y, x + 18, y + 18, 0xFF03090D);
        g.fill(x + 1, y + 1, x + 17, y + 17, accent);
        g.fill(x + 3, y + 3, x + 15, y + 15, 0xFF172431);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xEAF8FF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
        int statusY = menu.isAdvanced() ? 238 : 160;
        g.drawString(font, Component.translatable(statusKey(menu.getStatus())), 10, statusY, statusColor(menu.getStatus()), false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.luck_converter.stats",
                menu.getLuckLevel(), menu.getOperations(), menu.getEnergyPerTick()), 10, statusY + 10, 0x9CD8FF, false);
        g.drawString(font, Component.literal(menu.getEnergy() + " / " + menu.getEnergyCapacity() + " FE"),
                10, statusY + 20, 0xF4D27A, false);
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
