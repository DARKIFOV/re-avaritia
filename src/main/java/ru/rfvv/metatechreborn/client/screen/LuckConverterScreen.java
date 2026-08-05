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
        imageWidth = menu.isAdvanced() ? 368 : 332;
        imageHeight = menu.isAdvanced() ? 464 : 404;
        inventoryLabelX = menu.isAdvanced() ? 101 : 83;
        inventoryLabelY = menu.isAdvanced() ? 370 : 310;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick,
                            int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int inputColumns = advanced ? 12 : 10;
        int inputRows = advanced ? 6 : 3;
        int outputY = advanced ? 150 : 96;
        int sideX = advanced ? 238 : 202;
        int playerX = advanced ? 101 : 83;
        int playerY = advanced ? 382 : 322;
        int upgradesX = advanced ? 242 : 206;
        int utilityX = advanced ? 316 : 280;
        int sideWidth = imageWidth - sideX - 10;

        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20,
                inputColumns * 18 + 8, inputRows * 18 + 18);
        MetaTechGui.panel(g, leftPos + 6, topPos + outputY - 10, 170, 180);
        MetaTechGui.panel(g, leftPos + sideX - 4, topPos + 20,
                imageWidth - sideX - 2, 288);
        MetaTechGui.panel(g, leftPos + playerX - 4, topPos + playerY - 6, 170, 88);

        MetaTechGui.grid(g, leftPos + 10, topPos + 30,
                inputColumns, inputRows, 0xFF3A86B8);
        MetaTechGui.grid(g, leftPos + 10, topPos + outputY,
                9, 9, 0xFF7653A6);

        for (int index = 0; index < LuckConverterBlockEntity.UPGRADE_SLOTS; index++) {
            int column = index % 2;
            int row = index / 2;
            int accent = index < 3 ? 0xFF3A86B8 : 0xFFD89B2B;
            MetaTechGui.slot(g, leftPos + upgradesX + column * 24,
                    topPos + 42 + row * 26, accent);
        }
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 42, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 94, 0xFFD89B2B);

        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY,
                9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY + 58,
                9, 1, 0xFF777777);

        int progressY = 170;
        g.fill(leftPos + sideX, topPos + progressY,
                leftPos + sideX + sideWidth, topPos + progressY + 9, 0xFF555555);
        g.fill(leftPos + sideX + 1, topPos + progressY + 1,
                leftPos + sideX + 1 + menu.progressPixels(sideWidth - 2),
                topPos + progressY + 8, MetaTechGui.CYAN);

        int energyHeight = 68;
        int energyPixels = menu.energyPixels(energyHeight);
        g.fill(leftPos + utilityX + 22, topPos + 42,
                leftPos + utilityX + 29, topPos + 42 + energyHeight, 0xFF555555);
        g.fill(leftPos + utilityX + 23,
                topPos + 42 + energyHeight - energyPixels,
                leftPos + utilityX + 28,
                topPos + 42 + energyHeight, MetaTechGui.GOLD);

        int speedColor = menu.isInstantSpeed() ? 0xFF8E44AD
                : menu.getSpeedBonusPercent() >= 70 ? 0xFF3A9D72
                : menu.getSpeedBonusPercent() >= 30 ? 0xFF3A86B8
                : 0xFF777777;
        g.fill(leftPos + sideX, topPos + 144,
                leftPos + sideX + sideWidth, topPos + 150, 0xFF555555);
        int speedWidth = menu.isInstantSpeed()
                ? sideWidth
                : sideWidth * menu.getSpeedBonusPercent() / 100;
        g.fill(leftPos + sideX, topPos + 144,
                leftPos + sideX + speedWidth, topPos + 150, speedColor);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int outputY = advanced ? 150 : 96;
        int sideX = advanced ? 238 : 202;
        int utilityX = advanced ? 316 : 280;
        int sideWidth = imageWidth - sideX - 10;

        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.inputs"),
                10, 20, TEXT, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.outputs"),
                10, outputY - 10, TEXT, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.upgrades"),
                sideX, 20, TEXT, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.module"),
                utilityX - 8, 30, 0x176B45, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.energy_slot"),
                utilityX - 8, 82, 0x8A6200, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.speed_active"),
                sideX, 116, TEXT, false);
        g.drawString(font, speedText(), sideX, 132,
                menu.isInstantSpeed() ? 0x6B2E83 : 0x255C88, false);

        MetaTechGui.drawWrapped(g, font,
                Component.translatable(statusKey(menu.getStatus())),
                sideX, 188, sideWidth,
                statusColor(menu.getStatus()), 3);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable("gui.metatech_reborn.luck_converter.stats",
                        menu.getLuckLevel(), menu.getOperations(), menu.getEnergyPerTick()),
                sideX, 222, sideWidth, 0x255C88, 3);
        MetaTechGui.drawWrapped(g, font,
                Component.literal(menu.getEnergy() + " / "
                        + menu.getEnergyCapacity() + " FE"),
                sideX, 254, sideWidth, 0x8A6200, 2);
        g.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        boolean advanced = menu.isAdvanced();
        int sideX = advanced ? 238 : 202;
        int utilityX = advanced ? 316 : 280;
        int upgradesX = advanced ? 242 : 206;
        int sideWidth = imageWidth - sideX - 10;

        if (isInside(mouseX, mouseY, sideX, 170, sideWidth, 9)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, utilityX + 22, 42, 7, 68)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy",
                    menu.getEnergy(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, sideX, 144, sideWidth, 6)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.speed",
                    speedText()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, utilityX, 42, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.module"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, utilityX, 94, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy_slot"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, upgradesX, 42, 42, 70)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.luck_converter.tooltip.upgrades"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private Component speedText() {
        if (menu.isInstantSpeed()) {
            return Component.translatable(
                    "gui.metatech_reborn.luck_converter.speed.instant");
        }
        if (menu.getSpeedBonusPercent() <= 0) {
            return Component.translatable(
                    "gui.metatech_reborn.luck_converter.speed.none");
        }
        return Component.translatable(
                "gui.metatech_reborn.luck_converter.speed.percent",
                menu.getSpeedBonusPercent());
    }

    private static String statusKey(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING ->
                    "gui.metatech_reborn.luck_converter.status.running";
            case LuckConverterBlockEntity.STATUS_NO_MODULE ->
                    "gui.metatech_reborn.luck_converter.status.no_module";
            case LuckConverterBlockEntity.STATUS_NO_ENERGY ->
                    "gui.metatech_reborn.luck_converter.status.no_energy";
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL ->
                    "gui.metatech_reborn.luck_converter.status.output_full";
            case LuckConverterBlockEntity.STATUS_NO_VALID_INPUT ->
                    "gui.metatech_reborn.luck_converter.status.no_input";
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
