package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.menu.GreenhouseMenu;

public final class GreenhouseScreen extends AbstractContainerScreen<GreenhouseMenu> {
    public GreenhouseScreen(GreenhouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 286;
        imageHeight = 224;
        inventoryLabelX = 62;
        inventoryLabelY = 122;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE10232A);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 116, 0xFF183941);
        graphics.fill(leftPos + 55, topPos + 126, leftPos + 231, topPos + 220, 0xFF142B31);

        drawSlot(graphics, 20, 27);
        for (int column = 0; column < 3; column++) drawSlot(graphics, 20 + column * 24, 57);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) drawSlot(graphics, 118 + column * 18, 26 + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics, 62 + column * 18, 133 + row * 18);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics, 62 + column * 18, 191);

        graphics.fill(leftPos + 91, topPos + 29, leftPos + 181, topPos + 38, 0xFF0B1A1E);
        int progress = menu.getProgressPixels(88);
        graphics.fill(leftPos + 92, topPos + 30, leftPos + 92 + progress, topPos + 37, 0xFF63E6BE);

        graphics.fill(leftPos + 243, topPos + 24, leftPos + 254, topPos + 96, 0xFF07171D);
        int manaPixels = menu.getManaPixels(70);
        graphics.fill(leftPos + 244, topPos + 95 - manaPixels, leftPos + 253, topPos + 95, 0xFF49C6FF);

        graphics.fill(leftPos + 260, topPos + 24, leftPos + 271, topPos + 96, 0xFF07171D);
        int fluidPixels = menu.getFluidPixels(70);
        graphics.fill(leftPos + 261, topPos + 95 - fluidPixels, leftPos + 270, topPos + 95, 0xFF3A8DFF);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        int left = leftPos + x - 1;
        int top = topPos + y - 1;
        graphics.fill(left, top, left + 18, top + 18, 0xFF061519);
        graphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFF284A50);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xE8FFF8, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.flower"),
                8, 18, 0xB7DAD4, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.modules"),
                8, 47, 0xB7DAD4, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fuel"),
                112, 16, 0xB7DAD4, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB7DAD4, false);

        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.mana",
                        menu.getMana(), menu.getManaCapacity()),
                83, 49, 0x67D9FF, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fluid",
                        menu.getFluidAmount(), menu.getFluidCapacity()),
                83, 61, 0x79AFFF, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.levels",
                        menu.getSpeedLevel(), menu.getEfficiencyLevel(), menu.getEconomyLevel()),
                8, 103, 0xD9F4ED, false);
        graphics.drawString(font, Component.translatable(modeTranslationKey(menu.getModeId())),
                83, 76, 0xFFD56A, false);
    }

    private static String modeTranslationKey(int mode) {
        return switch (mode) {
            case 1 -> "gui.metatech_reborn.greenhouse.mode.endoflame";
            case 2 -> "gui.metatech_reborn.greenhouse.mode.hydroangeas";
            case 3 -> "gui.metatech_reborn.greenhouse.mode.gourmaryllis";
            case 4 -> "gui.metatech_reborn.greenhouse.mode.entropinnyum";
            case 5 -> "gui.metatech_reborn.greenhouse.mode.thermalily";
            case 6 -> "gui.metatech_reborn.greenhouse.mode.spectrolus";
            case 7 -> "gui.metatech_reborn.greenhouse.mode.custom";
            default -> "gui.metatech_reborn.greenhouse.mode.idle";
        };
    }
}
