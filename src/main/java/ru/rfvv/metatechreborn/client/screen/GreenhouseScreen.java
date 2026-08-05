package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.GreenhouseBlockEntity;
import ru.rfvv.metatechreborn.menu.GreenhouseMenu;

public final class GreenhouseScreen extends AbstractContainerScreen<GreenhouseMenu> {
    private static final int TEXT = 0x404040;
    private static final int MANA = 0xFF3A86B8;
    private static final int FLUID = 0xFF2674C9;
    private static final int TRACK = 0xFF3D4147;

    public GreenhouseScreen(GreenhouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 324;
        imageHeight = 282;
        inventoryLabelX = 81;
        inventoryLabelY = 182;
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
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 306, 148);
        MetaTechGui.panel(g, leftPos + 77, topPos + 188, 170, 88);

        MetaTechGui.slot(g, leftPos + 20, topPos + 34, 0xFF3A86B8);
        for (int column = 0; column < GreenhouseBlockEntity.MODULE_SLOTS; column++) {
            MetaTechGui.slot(g, leftPos + 20 + column * 24, topPos + 76, 0xFF7653A6);
        }
        MetaTechGui.grid(g, leftPos + 132, topPos + 34, 3, 2, 0xFFD89B2B);
        MetaTechGui.grid(g, leftPos + 81, topPos + 194, 9, 3, 0xFF777777);
        MetaTechGui.grid(g, leftPos + 81, topPos + 252, 9, 1, 0xFF777777);

        drawHorizontalBar(g, 14, 108, 284, 18, menu.getManaPixels(278), MANA);
        drawHorizontalBar(g, 14, 138, 284, 18, menu.getFluidPixels(278), FLUID);
    }

    private void drawHorizontalBar(GuiGraphics g, int x, int y, int width, int height,
                                   int filled, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF202329);
        g.fill(leftPos + x + 1, topPos + y + 1,
                leftPos + x + width - 1, topPos + y + height - 1, TRACK);
        int clamped = Math.max(0, Math.min(width - 6, filled));
        if (clamped > 0) {
            g.fill(leftPos + x + 3, topPos + y + 3,
                    leftPos + x + 3 + clamped, topPos + y + height - 3, color);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.mana_short",
                        menu.getMana(), menu.getManaCapacity()),
                20, 113, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fluid_short",
                        menu.getFluidAmount(), menu.getFluidCapacity()),
                20, 143, 0xFFFFFF, true);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 14, 108, 284, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 14, 138, 284, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.fluid",
                    menu.getFluidAmount(), menu.getFluidCapacity()), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
