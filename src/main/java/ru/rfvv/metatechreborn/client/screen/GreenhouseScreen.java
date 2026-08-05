package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.GreenhouseBlockEntity;
import ru.rfvv.metatechreborn.menu.GreenhouseMenu;

public final class GreenhouseScreen extends AbstractContainerScreen<GreenhouseMenu> {
    private static final int TEXT = 0x404040;
    private static final int MUTED = 0x606060;

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

        g.fill(leftPos + 112, topPos + 92, leftPos + 244, topPos + 102, 0xFF555555);
        int progress = menu.getProgressPixels(130);
        g.fill(leftPos + 113, topPos + 93, leftPos + 113 + progress, topPos + 101, 0xFF3A9D72);

        drawVerticalBar(g, 286, 34, 10, 80, menu.getManaPixels(78), 0xFF3A86B8);
        drawVerticalBar(g, 303, 34, 10, 80, menu.getFluidPixels(78), 0xFF2674C9);
    }

    private void drawVerticalBar(GuiGraphics g, int x, int y, int width, int height,
                                 int filled, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF555555);
        g.fill(leftPos + x + 1, topPos + y + height - 1 - filled,
                leftPos + x + width - 1, topPos + y + height - 1, color);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.flower"),
                12, 22, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.flower_stack"),
                12, 56, 0x255C88, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.modules"),
                12, 66, TEXT, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fuel"),
                126, 22, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);

        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.mana_short",
                        menu.getMana(), menu.getManaCapacity()),
                112, 54, 0x255C88, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fluid_short",
                        menu.getFluidAmount(), menu.getFluidCapacity()),
                112, 66, 0x245A9A, false);
        g.drawString(font, modeComponent(), 112, 108, 0x8A6200, false);
        MetaTechGui.drawWrapped(g, font, Component.translatable(statusTranslationKey(menu.getStatus())),
                112, 120, 164, statusColor(menu.getStatus()), 2);
        g.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.levels_short",
                        menu.getSpeedLevel(), menu.getEfficiencyLevel(), menu.getEconomyLevel()),
                12, 150, TEXT, false);
        g.drawString(font, Component.literal("M"), 287, 22, 0x255C88, false);
        g.drawString(font, Component.literal("F"), 304, 22, 0x245A9A, false);
    }

    private void renderMachineTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, 112, 92, 132, 10)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.tooltip.progress_ticks",
                    menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 286, 34, 10, 80)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.mana",
                    menu.getMana(), menu.getManaCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 303, 34, 10, 80)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.fluid",
                    menu.getFluidAmount(), menu.getFluidCapacity()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 20, 76,
                GreenhouseBlockEntity.MODULE_SLOTS * 24 - 6, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.modules",
                    menu.getSpeedLevel(), menu.getEfficiencyLevel(), menu.getEconomyLevel()),
                    mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, 20, 34, 18, 18)) {
            g.renderTooltip(font, Component.translatable(
                    "gui.metatech_reborn.greenhouse.tooltip.flower"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private Component modeComponent() {
        ItemStack flower = menu.getSlot(GreenhouseBlockEntity.FLOWER_SLOT).getItem();
        if (flower.isEmpty()) return Component.translatable("gui.metatech_reborn.greenhouse.mode.idle");
        Component flowerName = flower.getHoverName();
        if (menu.getModeId() == 6) {
            DyeColor color = DyeColor.byId(menu.getSpectrolusNextColor());
            return Component.empty().append(flowerName).append(": ")
                    .append(Component.translatable("color.minecraft." + color.getName()));
        }
        return flowerName;
    }

    private static String statusTranslationKey(int status) {
        return switch (status) {
            case GreenhouseBlockEntity.STATUS_NO_FLOWER -> "gui.metatech_reborn.greenhouse.status.no_flower";
            case GreenhouseBlockEntity.STATUS_UNSUPPORTED_FLOWER -> "gui.metatech_reborn.greenhouse.status.unsupported";
            case GreenhouseBlockEntity.STATUS_NO_FUEL -> "gui.metatech_reborn.greenhouse.status.no_fuel";
            case GreenhouseBlockEntity.STATUS_NO_FLUID -> "gui.metatech_reborn.greenhouse.status.no_fluid";
            case GreenhouseBlockEntity.STATUS_WRONG_TIME -> "gui.metatech_reborn.greenhouse.status.wrong_time";
            case GreenhouseBlockEntity.STATUS_MANA_FULL -> "gui.metatech_reborn.greenhouse.status.full";
            case GreenhouseBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.greenhouse.status.running";
            default -> "gui.metatech_reborn.greenhouse.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case GreenhouseBlockEntity.STATUS_RUNNING -> 0x176B45;
            case GreenhouseBlockEntity.STATUS_MANA_FULL -> 0x8A6200;
            case GreenhouseBlockEntity.STATUS_NO_FLOWER,
                 GreenhouseBlockEntity.STATUS_UNSUPPORTED_FLOWER,
                 GreenhouseBlockEntity.STATUS_NO_FUEL,
                 GreenhouseBlockEntity.STATUS_NO_FLUID,
                 GreenhouseBlockEntity.STATUS_WRONG_TIME -> 0xA02020;
            default -> MUTED;
        };
    }
}
