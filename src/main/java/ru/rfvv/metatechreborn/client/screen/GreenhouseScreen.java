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
    public GreenhouseScreen(GreenhouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 304;
        imageHeight = 238;
        inventoryLabelX = 70;
        inventoryLabelY = 135;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF010232A);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + 128, 0xFF183941);
        graphics.fill(leftPos + 61, topPos + 139, leftPos + 243, topPos + 234, 0xFF142B31);

        drawSlot(graphics, 18, 32);
        for (int column = 0; column < 3; column++) drawSlot(graphics, 18 + column * 24, 64);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) drawSlot(graphics, 122 + column * 18, 32 + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics, 70 + column * 18, 146 + row * 18);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics, 70 + column * 18, 204);

        graphics.fill(leftPos + 104, topPos + 78, leftPos + 224, topPos + 88, 0xFF07171D);
        int progress = menu.getProgressPixels(118);
        graphics.fill(leftPos + 105, topPos + 79, leftPos + 105 + progress, topPos + 87, 0xFF63E6BE);

        drawVerticalBar(graphics, 272, 29, 10, 76, menu.getManaPixels(74), 0xFF49C6FF);
        drawVerticalBar(graphics, 287, 29, 10, 76, menu.getFluidPixels(74), 0xFF3A8DFF);
    }

    private void drawVerticalBar(GuiGraphics graphics, int x, int y, int width, int height,
                                 int filled, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF07171D);
        graphics.fill(leftPos + x + 1, topPos + y + height - 1 - filled,
                leftPos + x + width - 1, topPos + y + height - 1, color);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        int left = leftPos + x - 1;
        int top = topPos + y - 1;
        graphics.fill(left, top, left + 18, top + 18, 0xFF061519);
        graphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFF284A50);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, 0xE8FFF8, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.flower"),
                8, 20, 0xB7DAD4, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.modules"),
                8, 52, 0xB7DAD4, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fuel"),
                116, 20, 0xB7DAD4, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xB7DAD4, false);

        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.mana_short",
                        menu.getMana(), menu.getManaCapacity()),
                104, 48, 0x67D9FF, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.fluid_short",
                        menu.getFluidAmount(), menu.getFluidCapacity()),
                104, 59, 0x79AFFF, false);
        graphics.drawString(font, modeComponent(),
                104, 93, 0xFFD56A, false);
        graphics.drawString(font, Component.translatable(statusTranslationKey(menu.getStatus())),
                104, 105, statusColor(menu.getStatus()), false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.greenhouse.levels_short",
                        menu.getSpeedLevel(), menu.getEfficiencyLevel(), menu.getEconomyLevel()),
                8, 116, 0xD9F4ED, false);
        graphics.drawString(font, Component.literal("M"), 273, 17, 0x67D9FF, false);
        graphics.drawString(font, Component.literal("F"), 288, 17, 0x79AFFF, false);
    }

    private Component modeComponent() {
        ItemStack flower = menu.getSlot(GreenhouseBlockEntity.FLOWER_SLOT).getItem();
        if (flower.isEmpty()) {
            return Component.translatable("gui.metatech_reborn.greenhouse.mode.idle");
        }
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
            case GreenhouseBlockEntity.STATUS_RUNNING -> 0xFF63E6BE;
            case GreenhouseBlockEntity.STATUS_MANA_FULL -> 0xFFFFD56A;
            case GreenhouseBlockEntity.STATUS_NO_FLOWER,
                 GreenhouseBlockEntity.STATUS_UNSUPPORTED_FLOWER,
                 GreenhouseBlockEntity.STATUS_NO_FUEL,
                 GreenhouseBlockEntity.STATUS_NO_FLUID,
                 GreenhouseBlockEntity.STATUS_WRONG_TIME -> 0xFFFF8A8A;
            default -> 0xFFB7DAD4;
        };
    }
}
