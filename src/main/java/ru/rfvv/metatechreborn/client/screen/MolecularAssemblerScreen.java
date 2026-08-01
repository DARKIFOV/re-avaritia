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
        imageWidth = 438;
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
                .bounds(leftPos + 176, topPos + 17, 72, 18)
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
        graphics.fill(panelLeft + 8, topPos + 12, panelRight - 8, topPos + 94, 0xFF0B1D24);
        graphics.fill(panelLeft + 8, topPos + 96, panelRight - 8, topPos + 129, 0xFF0B1D24);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = column + row * 9;
                int x = leftPos + 267 + column * 18;
                int y = topPos + 18 + row * 18;
                boolean active = slot < menu.getActivePatternSlots();
                graphics.fill(x, y, x + 18, y + 18, 0xFF061319);
                graphics.fill(x + 1, y + 1, x + 17, y + 17,
                        active ? 0xFF37515A : 0xFF1D292D);
                if (!active) graphics.fill(x + 4, y + 4, x + 14, y + 14, 0xAA000000);
            }
        }

        graphics.fill(leftPos + 267, topPos + 101, leftPos + 285, topPos + 119, 0xFF061319);
        graphics.fill(leftPos + 268, topPos + 102, leftPos + 284, topPos + 118, 0xFF37515A);

        int progress = menu.getProgressPixels(66);
        graphics.fill(leftPos + 177, topPos + 112, leftPos + 177 + progress, topPos + 120, 0xFF43D7FF);

        int energyPixels = menu.getEnergyPixels(51);
        graphics.fill(leftPos + 236, topPos + 100 - energyPixels,
                leftPos + 244, topPos + 101, 0xFFFFC857);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xEAF8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
        graphics.drawString(font,
                Component.translatable(menu.isRecipeLocked()
                        ? "gui.metatech_reborn.recipe_locked"
                        : "gui.metatech_reborn.recipe_unlocked"),
                176, 40, menu.isRecipeLocked() ? 0x78F0A2 : 0xFF8A8A, false);
        graphics.drawString(font,
                Component.literal(menu.getEnergyStored() + " / " + menu.getEnergyCapacity() + " FE"),
                176, 128, 0xF4D27A, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.ae2_native_patterns"),
                176, 144, 0x9CCBFF, false);

        graphics.drawString(font, Component.translatable("gui.metatech_reborn.pattern_bank"),
                268, 5, 0xEAF8FF, false);
        graphics.drawString(font,
                Component.translatable("gui.metatech_reborn.pattern_count",
                        menu.getInstalledPatternCount(), menu.getActivePatternSlots()),
                291, 103, 0x9CCBFF, false);
        graphics.drawString(font,
                Component.translatable(menu.getActivePatternSlots() == MolecularAssemblerBlockEntity.MAX_PATTERN_SLOTS
                        ? "gui.metatech_reborn.pattern_capacity.full"
                        : "gui.metatech_reborn.pattern_capacity.base"),
                268, 123, 0xBBD5E7, false);
    }
}
