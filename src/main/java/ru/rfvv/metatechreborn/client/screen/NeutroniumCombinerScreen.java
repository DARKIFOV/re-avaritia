package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;

public final class NeutroniumCombinerScreen extends AbstractContainerScreen<NeutroniumCombinerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/neutronium_combiner.png");

    public NeutroniumCombinerScreen(NeutroniumCombinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 286;
        imageHeight = 238;
        inventoryLabelX = 62;
        inventoryLabelY = 146;
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

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int input = column + row * 3;
                int x = leftPos + 10 + column * 28;
                int y = topPos + 22 + row * 28;
                int pixels = menu.getProgressPixels(input, 16);
                graphics.fill(x + 1, y + 20, x + 1 + pixels, y + 22, 0xFF42D7F5);
                graphics.fill(x + 20, y + 1, x + 23, y + 4, statusColor(menu.getStatus(input)));
            }
        }

        int energyPixels = menu.getEnergyPixels(82);
        graphics.fill(leftPos + 95, topPos + 103 - energyPixels,
                leftPos + 101, topPos + 104, 0xFFFFC857);
        graphics.fill(leftPos + 97, topPos + 115, leftPos + 117, topPos + 135, 0xFF07171D);
        graphics.fill(leftPos + 98, topPos + 116, leftPos + 116, topPos + 134, 0xFF29434B);
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
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, 0xEAF8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
        graphics.drawString(font,
                Component.literal(menu.getEnergyStored() + " / " + menu.getEnergyCapacity() + " FE"),
                120, 116, 0xF4D27A, false);
        graphics.drawString(font,
                Component.translatable("gui.metatech_reborn.neutron_upgrades",
                        menu.getSpeedUpgrades(), menu.getEfficiencyUpgrades(), menu.getOutputUpgrades()),
                106, 130, 0x9CCBFF, false);
    }
}
