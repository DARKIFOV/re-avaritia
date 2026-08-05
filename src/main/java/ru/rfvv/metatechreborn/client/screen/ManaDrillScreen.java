package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x606060;

    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 324;
        imageHeight = 324;
        inventoryLabelX = 81;
        inventoryLabelY = 228;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 196);
        MetaTechGui.panel(g, leftPos + 124, topPos + 20, 194, 176);
        MetaTechGui.panel(g, leftPos + 77, topPos + 234, 170, 84);

        MetaTechGui.slot(g, leftPos + 20, topPos + 34, 0xFF3A9D72);
        MetaTechGui.slot(g, leftPos + 20, topPos + 64, 0xFF3A86B8);
        MetaTechGui.slot(g, leftPos + 44, topPos + 64, 0xFF7653A6);
        MetaTechGui.slot(g, leftPos + 68, topPos + 64, 0xFFD89B2B);
        MetaTechGui.grid(g, leftPos + 132, topPos + 30, 9, 9, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 240, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 298, 9, 1, 0xFF777777);

        int manaPixels = menu.getManaPixels(80);
        g.fill(leftPos + 104, topPos + 32, leftPos + 112, topPos + 114, 0xFF555555);
        g.fill(leftPos + 105, topPos + 113 - manaPixels,
                leftPos + 111, topPos + 113, 0xFF3A86B8);

        int progressPixels = menu.getProgressPixels(96);
        g.fill(leftPos + 12, topPos + 124, leftPos + 110, topPos + 134, 0xFF555555);
        g.fill(leftPos + 13, topPos + 125, leftPos + 13 + progressPixels,
                topPos + 133, 0xFF7653A6);
        g.fill(leftPos + 12, topPos + 144, leftPos + 20, topPos + 152,
                statusColor(menu.getStatus()));
    }

    @Override protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.module"),
                12, 22, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.upgrades"),
                12, 54, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.outputs"),
                128, 22, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.mana",
                        menu.getMana(), menu.getManaCapacity()),
                12, 96, 0x255C88, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.levels",
                        menu.getSpeedLevel(), menu.getLootingLevel(), menu.getGenerationLevel()),
                12, 108, TEXT, false);
        MetaTechGui.drawWrapped(g, font, Component.translatable(statusKey(menu.getStatus())),
                24, 142, 86, statusColor(menu.getStatus()), 3);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable(menu.isStructureFormed()
                        ? "gui.metatech_reborn.structure_formed"
                        : "gui.metatech_reborn.structure_missing"),
                12, 174, 98, menu.isStructureFormed() ? 0x176B45 : 0xA02020, 2);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 104, 32, 8, 82)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.mana_drill.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 12, 124, 98, 10)) {
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
        } else if (isInside(mouseX, mouseY, 12, 144, 8, 8)) {
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
            case ManaDrillBlockEntity.STATUS_RUNNING -> 0x176B45;
            case ManaDrillBlockEntity.STATUS_NO_MANA -> 0x8A6200;
            case ManaDrillBlockEntity.STATUS_STRUCTURE_MISSING,
                 ManaDrillBlockEntity.STATUS_NO_MODULE,
                 ManaDrillBlockEntity.STATUS_NO_RECIPE,
                 ManaDrillBlockEntity.STATUS_OUTPUT_FULL -> 0xA02020;
            default -> MUTED;
        };
    }
}
