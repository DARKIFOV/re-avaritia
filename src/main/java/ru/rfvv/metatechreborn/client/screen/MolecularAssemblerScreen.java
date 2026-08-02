package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;

public final class MolecularAssemblerScreen extends AbstractContainerScreen<MolecularAssemblerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/molecular_assembler_9x9.png");

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 430;
        imageHeight = 256;
        inventoryLabelX = 8;
        inventoryLabelY = 168;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.metatech_reborn.unlock_recipe"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, MolecularAssemblerMenu.UNLOCK_BUTTON_ID);
                            }
                        })
                .bounds(leftPos + 170, topPos + 16, 82, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 256, imageHeight, 512, 512);

        int panelLeft = leftPos + 256;
        int panelRight = leftPos + imageWidth;
        graphics.fill(panelLeft, topPos, panelRight, topPos + imageHeight, 0xFF10242C);
        graphics.fill(panelLeft + 2, topPos + 2, panelRight - 2, topPos + imageHeight - 2, 0xFF17343E);
        graphics.fill(panelLeft + 9, topPos + 16, panelRight - 9, topPos + 96, 0xFF0B1D24);
        graphics.fill(panelLeft + 9, topPos + 99, panelRight - 9, topPos + 142, 0xFF0B1D24);

        for (int slot = 0; slot < MolecularAssemblerBlockEntity.MAX_PATTERN_SLOTS; slot++) {
            int column = slot % MolecularAssemblerMenu.PATTERN_COLUMNS;
            int row = slot / MolecularAssemblerMenu.PATTERN_COLUMNS;
            int x = leftPos + 280 + column * 18;
            int y = topPos + 21 + row * 18;
            boolean active = slot < menu.getActivePatternSlots();
            graphics.fill(x, y, x + 18, y + 18, 0xFF061319);
            graphics.fill(x + 1, y + 1, x + 17, y + 17,
                    active ? 0xFF37515A : 0xFF1D292D);
            if (!active) {
                graphics.fill(x + 4, y + 4, x + 14, y + 14, 0xAA000000);
                graphics.fill(x + 7, y + 5, x + 11, y + 13, 0xFF26363C);
            }
        }

        drawSlot(graphics, 281, 105, true);
        drawSlot(graphics, 191, 74, true);
        drawSlot(graphics, 218, 74, true);

        int progress = menu.getProgressPixels(66);
        graphics.fill(leftPos + 177, topPos + 112, leftPos + 243, topPos + 121, 0xFF07171D);
        graphics.fill(leftPos + 178, topPos + 113, leftPos + 178 + progress, topPos + 120, 0xFF43D7FF);

        int energyPixels = menu.getEnergyPixels(51);
        graphics.fill(leftPos + 238, topPos + 50, leftPos + 246, topPos + 102, 0xFF07171D);
        graphics.fill(leftPos + 239, topPos + 101 - energyPixels,
                leftPos + 245, topPos + 101, 0xFFFFC857);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, boolean active) {
        graphics.fill(leftPos + x - 1, topPos + y - 1,
                leftPos + x + 17, topPos + y + 17, 0xFF061319);
        graphics.fill(leftPos + x, topPos + y,
                leftPos + x + 16, topPos + y + 16, active ? 0xFF37515A : 0xFF1D292D);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xEAF8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);

        graphics.drawString(font,
                Component.translatable(menu.isRecipeLocked()
                        ? "gui.metatech_reborn.recipe_locked"
                        : "gui.metatech_reborn.recipe_unlocked"),
                170, 40, menu.isRecipeLocked() ? 0x78F0A2 : 0xFF8A8A, false);
        graphics.drawString(font, Component.translatable(statusKey(menu.getStatus())),
                170, 52, statusColor(menu.getStatus()), false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.assembler.output"),
                179, 64, 0xBBD5E7, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.assembler.charge"),
                210, 64, 0xBBD5E7, false);
        graphics.drawString(font,
                Component.translatable("gui.metatech_reborn.assembler.energy",
                        menu.getEnergyStored(), menu.getEnergyCapacity()),
                170, 128, 0xF4D27A, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.ae2_native_patterns"),
                170, 141, 0x9CCBFF, false);

        graphics.drawString(font, Component.translatable("gui.metatech_reborn.pattern_bank"),
                266, 6, 0xEAF8FF, false);
        graphics.drawString(font,
                Component.translatable("gui.metatech_reborn.pattern_count",
                        menu.getInstalledPatternCount(), menu.getActivePatternSlots()),
                306, 107, 0x9CCBFF, false);
        graphics.drawString(font,
                Component.translatable(menu.getActivePatternSlots() == MolecularAssemblerBlockEntity.MAX_PATTERN_SLOTS
                        ? "gui.metatech_reborn.pattern_capacity.full"
                        : "gui.metatech_reborn.pattern_capacity.base"),
                266, 127, 0xBBD5E7, false);
    }

    private static String statusKey(int status) {
        return switch (status) {
            case MolecularAssemblerBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.assembler.status.recipe";
            case MolecularAssemblerBlockEntity.STATUS_NO_ENERGY -> "gui.metatech_reborn.assembler.status.energy";
            case MolecularAssemblerBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.assembler.status.output";
            case MolecularAssemblerBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.assembler.status.running";
            case MolecularAssemblerBlockEntity.STATUS_AE2_READY -> "gui.metatech_reborn.assembler.status.ae2_ready";
            case MolecularAssemblerBlockEntity.STATUS_RETURNING_TO_NETWORK ->
                    "gui.metatech_reborn.assembler.status.returning";
            default -> "gui.metatech_reborn.assembler.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case MolecularAssemblerBlockEntity.STATUS_RUNNING -> 0xFF52E389;
            case MolecularAssemblerBlockEntity.STATUS_AE2_READY -> 0xFF6ED7FF;
            case MolecularAssemblerBlockEntity.STATUS_RETURNING_TO_NETWORK -> 0xFFB68CFF;
            case MolecularAssemblerBlockEntity.STATUS_NO_ENERGY -> 0xFFFFD56A;
            case MolecularAssemblerBlockEntity.STATUS_NO_RECIPE,
                 MolecularAssemblerBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF7382;
            default -> 0xFFBBD5E7;
        };
    }
}
