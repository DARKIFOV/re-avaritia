package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;

public final class MolecularAssemblerScreen extends AbstractContainerScreen<MolecularAssemblerMenu> {
    private Button unlockButton;

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 484;
        imageHeight = 286;
        inventoryLabelX = 10;
        inventoryLabelY = 190;
    }

    @Override
    protected void init() {
        super.init();
        unlockButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.metatech_reborn.unlock_recipe"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, MolecularAssemblerMenu.UNLOCK_BUTTON_ID);
                            }
                        })
                .bounds(leftPos + 186, topPos + 28, 100, 20)
                .build());
        updateUnlockButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateUnlockButton();
    }

    private void updateUnlockButton() {
        if (unlockButton != null) {
            unlockButton.active = menu.isRecipeLocked();
            unlockButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable(menu.isRecipeLocked()
                            ? "gui.metatech_reborn.assembler.tooltip.unlock"
                            : "gui.metatech_reborn.assembler.tooltip.unlocked")));
        }
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
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 166, 166);
        MetaTechGui.panel(g, leftPos + 178, topPos + 6, 114, 180);
        MetaTechGui.panel(g, leftPos + 296, topPos + 6, 182, 180);
        MetaTechGui.panel(g, leftPos + 6, topPos + 196, 166, 84);

        MetaTechGui.grid(g, leftPos + 10, topPos + 26, 9, 9, 0xFF48BFE3);
        MetaTechGui.grid(g, leftPos + 10, topPos + 202, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 10, topPos + 260, 9, 1, 0xFF73879A);

        MetaTechGui.slot(g, leftPos + 194, topPos + 72, 0xFF7A5DE8);
        MetaTechGui.slot(g, leftPos + 226, topPos + 72, 0xFFFFC857);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                int accent = slot < menu.getActivePatternSlots() ? 0xFF48BFE3 : 0xFF33444F;
                MetaTechGui.slot(g, leftPos + 304 + column * 18, topPos + 28 + row * 18, accent);
                if (slot >= menu.getActivePatternSlots()) {
                    g.fill(leftPos + 308 + column * 18, topPos + 32 + row * 18,
                            leftPos + 316 + column * 18, topPos + 40 + row * 18, 0xAA000000);
                }
            }
        }
        MetaTechGui.slot(g, leftPos + 304, topPos + 110, 0xFF7A5DE8);
        for (int slot = 0; slot < MolecularAssemblerBlockEntity.AE2_SPEED_CARD_SLOTS; slot++) {
            MetaTechGui.slot(g, leftPos + 334 + slot * 20, topPos + 110, 0xFF48BFE3);
        }

        int progress = menu.getProgressPixels(92);
        g.fill(leftPos + 188, topPos + 103, leftPos + 282, topPos + 113, 0xFF03090D);
        g.fill(leftPos + 189, topPos + 104,
                leftPos + 189 + progress, topPos + 112, MetaTechGui.CYAN);

        int energy = menu.getEnergyPixels(92);
        g.fill(leftPos + 188, topPos + 119, leftPos + 282, topPos + 129, 0xFF03090D);
        g.fill(leftPos + 189, topPos + 120,
                leftPos + 189 + energy, topPos + 128, MetaTechGui.GOLD);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);

        g.drawString(font, Component.translatable("gui.metatech_reborn.assembler.output"),
                188, 59, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.assembler.charge"),
                224, 59, 0xBBD5E7, false);
        g.drawString(font, Component.translatable(menu.isRecipeLocked()
                        ? "gui.metatech_reborn.recipe_locked"
                        : "gui.metatech_reborn.recipe_unlocked"),
                186, 51, menu.isRecipeLocked() ? 0x78F0A2 : 0xFF8A8A, false);

        int status = menu.getStatus();
        if (status != MolecularAssemblerBlockEntity.STATUS_IDLE
                && status != MolecularAssemblerBlockEntity.STATUS_AE2_READY) {
            MetaTechGui.drawWrapped(g, font, Component.translatable(statusKey(status)),
                    186, 137, 98, statusColor(status), 3);
        }
        g.drawString(font, Component.translatable("gui.metatech_reborn.assembler.speed_cards",
                        menu.getAe2SpeedCards()),
                186, 166, 0x6ED7FF, false);

        g.drawString(font, Component.translatable("gui.metatech_reborn.pattern_bank"),
                302, 10, 0xEAF8FF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.pattern_count",
                        menu.getInstalledPatternCount(), menu.getActivePatternSlots()),
                304, 134, 0x9CCBFF, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.pattern_capacity_slot"),
                304, 146, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.ae2_speed_cards"),
                334, 122, 0x6ED7FF, false);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable("gui.metatech_reborn.ae2_native_patterns"),
                304, 158, 166, 0x9CCBFF, 2);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 188, 103, 94, 10)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 188, 119, 94, 10)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.energy",
                    menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 304, 28, 162, 72)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.assembler.tooltip.pattern_bank",
                    menu.getInstalledPatternCount(), menu.getActivePatternSlots()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 304, 110, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.assembler.tooltip.capacity"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 334, 110, 78, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.assembler.tooltip.speed_cards",
                    menu.getAe2SpeedCards()), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static String statusKey(int status) {
        return switch (status) {
            case MolecularAssemblerBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.assembler.status.recipe";
            case MolecularAssemblerBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.assembler.status.energy";
            case MolecularAssemblerBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.assembler.status.output";
            case MolecularAssemblerBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.assembler.status.running";
            case MolecularAssemblerBlockEntity.STATUS_AE2_READY -> "gui.metatech_reborn.assembler.status.ae2_ready";
            default -> "gui.metatech_reborn.assembler.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case MolecularAssemblerBlockEntity.STATUS_RUNNING -> 0xFF52E389;
            case MolecularAssemblerBlockEntity.STATUS_AE2_READY -> 0xFF6ED7FF;
            case MolecularAssemblerBlockEntity.STATUS_NO_ENERGY -> 0xFFFFD56A;
            case MolecularAssemblerBlockEntity.STATUS_NO_RECIPE,
                 MolecularAssemblerBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF7382;
            default -> 0xFFBBD5E7;
        };
    }
}
