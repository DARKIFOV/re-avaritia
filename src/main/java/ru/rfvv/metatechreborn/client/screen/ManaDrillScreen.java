package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 324;
        imageHeight = 270;
        inventoryLabelX = 81;
        inventoryLabelY = 170;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 132);
        MetaTechGui.panel(g, leftPos + 124, topPos + 20, 194, 132);
        MetaTechGui.panel(g, leftPos + 77, topPos + 176, 170, 88);

        MetaTechGui.slot(g, leftPos + 20, topPos + 34, 0xFF63E6BE);
        MetaTechGui.slot(g, leftPos + 20, topPos + 64, 0xFF48BFE3);
        MetaTechGui.slot(g, leftPos + 44, topPos + 64, 0xFF7A5DE8);
        MetaTechGui.slot(g, leftPos + 68, topPos + 64, 0xFFFFA43A);
        MetaTechGui.grid(g, leftPos + 132, topPos + 30, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 81, topPos + 182, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 81, topPos + 240, 9, 1, 0xFF73879A);

        int manaPixels = menu.getManaPixels(62);
        g.fill(leftPos + 104, topPos + 32, leftPos + 112, topPos + 96, 0xFF03090D);
        g.fill(leftPos + 105, topPos + 95 - manaPixels,
                leftPos + 111, topPos + 95, MetaTechGui.CYAN);

        int progressPixels = menu.getProgressPixels(180);
        g.fill(leftPos + 132, topPos + 96, leftPos + 314, topPos + 106, 0xFF03090D);
        g.fill(leftPos + 133, topPos + 97, leftPos + 133 + progressPixels,
                topPos + 105, MetaTechGui.PURPLE);
        g.fill(leftPos + 132, topPos + 116, leftPos + 140, topPos + 124,
                statusColor(menu.getStatus()));
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.module"),
                12, 22, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.upgrades"),
                12, 54, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.outputs"),
                128, 22, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.mana",
                        menu.getMana(), menu.getManaCapacity()),
                12, 96, 0x6EE7F9, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.levels",
                        menu.getSpeedLevel(), menu.getLootingLevel(), menu.getGenerationLevel()),
                12, 108, 0xD8E6F3, false);
        MetaTechGui.drawWrapped(g, font, Component.translatable(statusKey(menu.getStatus())),
                144, 114, 166, statusColor(menu.getStatus()), 2);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable(menu.isStructureFormed()
                        ? "gui.metatech_reborn.structure_formed"
                        : "gui.metatech_reborn.structure_missing"),
                12, 128, 100, menu.isStructureFormed() ? 0x55E58A : 0xFF7382, 2);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 104, 32, 8, 64)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 132, 96, 182, 10)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 20, 34, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.module"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 20, 64, 66, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.upgrades",
                    menu.getSpeedLevel(), menu.getLootingLevel(), menu.getGenerationLevel()),
                    mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 132, 116, 8, 8)) {
            g.renderTooltip(font, Component.translatable(statusKey(menu.getStatus())), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static String statusKey(int status) {
        return switch (status) {
            case ManaDrillBlockEntity.STATUS_STRUCTURE_MISSING -> "gui.metatech_reborn.mana_drill.status.structure";
            case ManaDrillBlockEntity.STATUS_NO_MODULE -> "gui.metatech_reborn.mana_drill.status.module";
            case ManaDrillBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.mana_drill.status.recipe";
            case ManaDrillBlockEntity.STATUS_NO_MANA -> "gui.metatech_reborn.mana_drill.status.mana";
            case ManaDrillBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.mana_drill.status.output";
            case ManaDrillBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.mana_drill.status.running";
            default -> "gui.metatech_reborn.mana_drill.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case ManaDrillBlockEntity.STATUS_RUNNING -> 0xFF52E389;
            case ManaDrillBlockEntity.STATUS_NO_MANA -> 0xFFFFD56A;
            case ManaDrillBlockEntity.STATUS_STRUCTURE_MISSING,
                 ManaDrillBlockEntity.STATUS_NO_MODULE,
                 ManaDrillBlockEntity.STATUS_NO_RECIPE,
                 ManaDrillBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF7382;
            default -> 0xFF9CCBFF;
        };
    }
}
