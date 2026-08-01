package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;

public final class MolecularAssemblerScreen extends AbstractContainerScreen<MolecularAssemblerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/molecular_assembler_9x9.png");

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
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
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 512);

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
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.ae2_direct"),
                176, 144, 0x9CCBFF, false);
    }
}
