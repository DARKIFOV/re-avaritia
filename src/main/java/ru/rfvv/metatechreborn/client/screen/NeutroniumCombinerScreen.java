package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;

public final class NeutroniumCombinerScreen extends AbstractContainerScreen<NeutroniumCombinerMenu> {
    public NeutroniumCombinerScreen(NeutroniumCombinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 344;
        imageHeight = 282;
        inventoryLabelX = 91;
        inventoryLabelY = 184;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 112, 104);
        MetaTechGui.panel(g, leftPos + 120, topPos + 20, 218, 104);
        MetaTechGui.panel(g, leftPos + 6, topPos + 126, 112, 48);
        MetaTechGui.panel(g, leftPos + 120, topPos + 126, 218, 48);
        MetaTechGui.panel(g, leftPos + 87, topPos + 190, 170, 86);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int input = column + row * 3;
                int x = leftPos + 12 + column * 30;
                int y = topPos + 30 + row * 30;
                MetaTechGui.slot(g, x, y, 0xFF48BFE3);
                int pixels = menu.getProgressPixels(input, 16);
                g.fill(x, y + 18, x + 16, y + 21, 0xFF03090D);
                g.fill(x, y + 18, x + pixels, y + 21, MetaTechGui.CYAN);
                g.fill(x + 18, y, x + 22, y + 4, statusColor(menu.getStatus(input)));
            }
        }

        MetaTechGui.grid(g, leftPos + 126, topPos + 28, 8, 5, 0xFF73879A);
        for (int column = 0; column < NeutroniumCombinerBlockEntity.UPGRADE_SLOTS; column++) {
            MetaTechGui.slot(g, leftPos + 12 + column * 22, topPos + 130, 0xFFFFA43A);
        }
        MetaTechGui.slot(g, leftPos + 104, topPos + 130, 0xFFFFC857);
        MetaTechGui.grid(g, leftPos + 91, topPos + 196, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 91, topPos + 254, 9, 1, 0xFF73879A);

        int energyPixels = menu.getEnergyPixels(38);
        g.fill(leftPos + 326, topPos + 132, leftPos + 334, topPos + 172, 0xFF03090D);
        g.fill(leftPos + 327, topPos + 171 - energyPixels,
                leftPos + 333, topPos + 171, MetaTechGui.GOLD);
    }

    private static int statusColor(int status) {
        return switch (status) {
            case NeutroniumCombinerBlockEntity.STATUS_RUNNING -> 0xFF4EE08A;
            case NeutroniumCombinerBlockEntity.STATUS_NO_RECIPE -> 0xFFFF4F67;
            case NeutroniumCombinerBlockEntity.STATUS_NO_ENERGY -> 0xFFFFC857;
            case NeutroniumCombinerBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF8B45;
            default -> 0xFF53666D;
        };
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.collectors"),
                10, 22, 0x9CCBFF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.outputs"),
                124, 22, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.stack_hint"),
                10, 116, 0x6ED7FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.upgrades"),
                10, 128, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.neutron.energy",
                        menu.getEnergyStored(), menu.getEnergyCapacity()),
                124, 132, 0xF4D27A, false);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable("gui.metatech_reborn.neutron_upgrades",
                        menu.getSpeedUpgrades(), menu.getEfficiencyUpgrades(), menu.getOutputUpgrades()),
                124, 146, 190, 0x9CCBFF, 2);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int input = column + row * 3;
                int x = 12 + column * 30;
                int y = 30 + row * 30;
                if (isInside(mouseX, mouseY, x, y, 22, 22)) {
                    g.renderTooltip(font, Component.translatable(
                            "gui.metatech_reborn.neutron.tooltip.process",
                            input + 1, menu.getProgress(input), menu.getMaxProgress(input),
                            Component.translatable(statusKey(menu.getStatus(input)))),
                            mouseX, mouseY);
                    return;
                }
            }
        }
        if (isInside(mouseX, mouseY, 326, 132, 8, 40)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy",
                    menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 12, 130, 84, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.neutron.tooltip.upgrades",
                    menu.getSpeedUpgrades(), menu.getEfficiencyUpgrades(), menu.getOutputUpgrades()),
                    mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static String statusKey(int status) {
        return switch (status) {
            case NeutroniumCombinerBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.neutron.status.running";
            case NeutroniumCombinerBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.neutron.status.recipe";
            case NeutroniumCombinerBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.neutron.status.energy";
            case NeutroniumCombinerBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.neutron.status.output";
            default -> "gui.metatech_reborn.neutron.status.idle";
        };
    }
}
