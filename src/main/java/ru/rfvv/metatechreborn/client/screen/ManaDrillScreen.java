package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.menu.ManaDrillMenu;

public final class ManaDrillScreen extends AbstractContainerScreen<ManaDrillMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/mana_drill.png");

    public ManaDrillScreen(ManaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 286;
        imageHeight = 224;
        inventoryLabelX = 62;
        inventoryLabelY = 126;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 512);

        int manaPixels = menu.getManaPixels(54);
        graphics.fill(leftPos + 11, topPos + 72 - manaPixels,
                leftPos + 15, topPos + 73, 0xFF42D7F5);

        int progressPixels = menu.getProgressPixels(72);
        graphics.fill(leftPos + 18, topPos + 78,
                leftPos + 18 + progressPixels, topPos + 82, 0xFFB26CFF);

        graphics.fill(leftPos + 18, topPos + 96, leftPos + 25, topPos + 103,
                statusColor(menu.getStatus()));
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, 0xEAF8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.mana",
                        menu.getMana(), menu.getManaCapacity()),
                102, 6, 0x6EE7F9, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.mana_drill.levels",
                        menu.getSpeedLevel(), menu.getLootingLevel(), menu.getGenerationLevel()),
                8, 108, 0xD8E6F3, false);
        graphics.drawString(font, Component.translatable(statusKey(menu.getStatus())),
                30, 95, statusColor(menu.getStatus()), false);
    }

    private static String statusKey(int status) {
        return switch (status) {
            case ManaDrillBlockEntity.STATUS_STRUCTURE_MISSING -> "gui.metatech_reborn.mana_drill.status.structure";
            case ManaDrillBlockEntity.STATUS_NO_MODULE -> "gui.metatech_reborn.mana_drill.status.module";
            case ManaDrillBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.mana_drill.status.recipe";
            case ManaDrillBlockEntity.STATUS_NO_MANA -> "gui.metatech_reborn.mana_drill.status.mana";
            case ManaDrillBlockEntity.STATUS_OUTPUT_FULL -> "gui.metatech_reborn.mana_drill.status.output";
            case ManaDrillBlockEntity.STATUS_RUNNING -> "gui.metatech_reborn.mana_drill.status.running";
            default -> "gui.metatech_reborn.mana_drill.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case ManaDrillBlockEntity.STATUS_RUNNING -> 0xFF52E389;
            case ManaDrillBlockEntity.STATUS_NO_MANA -> 0xFFFFD56A;
            case ManaDrillBlockEntity.STATUS_STRUCTURE_MISSING,
                 ManaDrillBlockEntity.STATUS_NO_MODULE,
                 ManaDrillBlockEntity.STATUS_NO_RECIPE,
                 ManaDrillBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF7382;
            default -> 0xFF9CCBFF;
        };
    }
}
