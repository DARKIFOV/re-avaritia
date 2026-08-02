package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;

public final class ExtremePatternEncoderScreen extends AbstractContainerScreen<ExtremePatternEncoderMenu> {
    public ExtremePatternEncoderScreen(ExtremePatternEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 330;
        imageHeight = 286;
        inventoryLabelX = 10;
        inventoryLabelY = 190;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.encode_pattern"),
                        button -> sendButton(ExtremePatternEncoderMenu.ENCODE_BUTTON_ID))
                .bounds(leftPos + 224, topPos + 26, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.clear_pattern_grid"),
                        button -> sendButton(ExtremePatternEncoderMenu.CLEAR_BUTTON_ID))
                .bounds(leftPos + 224, topPos + 136, 90, 20).build());
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        MetaTechGui.background(g, leftPos, topPos, imageWidth, imageHeight);
        MetaTechGui.panel(g, leftPos + 6, topPos + 20, 166, 166);
        MetaTechGui.panel(g, leftPos + 180, topPos + 6, 144, 180);
        MetaTechGui.panel(g, leftPos + 6, topPos + 196, 166, 84);

        MetaTechGui.grid(g, leftPos + 10, topPos + 26, 9, 9, 0xFF48BFE3);
        MetaTechGui.grid(g, leftPos + 10, topPos + 202, 9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + 10, topPos + 260, 9, 1, 0xFF73879A);
        MetaTechGui.slot(g, leftPos + 198, topPos + 64, 0xFF7A5DE8);
        MetaTechGui.slot(g, leftPos + 198, topPos + 106, 0xFF48BFE3);

        g.fill(leftPos + 224, topPos + 78, leftPos + 306, topPos + 88, 0xFF03090D);
        g.fill(leftPos + 225, topPos + 79, leftPos + 305, topPos + 87,
                menu.hasValidRecipe() ? 0xFF4EE08A : 0xFFFF4F67);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);

        g.drawString(font, Component.translatable("gui.metatech_reborn.blank_pattern"),
                190, 52, 0xBBD5E7, false);
        g.drawString(font, Component.translatable("gui.metatech_reborn.encoded_pattern"),
                190, 94, 0xBBD5E7, false);
        MetaTechGui.drawWrapped(g, font, statusText(menu.getStatus()),
                188, 162, 128, statusColor(menu.getStatus()), 2);
        MetaTechGui.drawWrapped(g, font,
                Component.translatable("gui.metatech_reborn.encoder.jei_hint"),
                188, 112, 128, 0x9CCBFF, 2);
    }

    private static Component statusText(int status) {
        return Component.translatable(switch (status) {
            case ExtremePatternEncoderBlockEntity.STATUS_ENCODED -> "gui.metatech_reborn.encoder.encoded";
            case ExtremePatternEncoderBlockEntity.STATUS_NO_RECIPE -> "gui.metatech_reborn.encoder.no_recipe";
            case ExtremePatternEncoderBlockEntity.STATUS_NO_BLANK -> "gui.metatech_reborn.encoder.no_blank";
            case ExtremePatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED -> "gui.metatech_reborn.encoder.output_blocked";
            default -> "gui.metatech_reborn.encoder.ready";
        });
    }

    private static int statusColor(int status) {
        return switch (status) {
            case ExtremePatternEncoderBlockEntity.STATUS_ENCODED -> 0x78F0A2;
            case ExtremePatternEncoderBlockEntity.STATUS_NO_RECIPE,
                 ExtremePatternEncoderBlockEntity.STATUS_NO_BLANK,
                 ExtremePatternEncoderBlockEntity.STATUS_OUTPUT_BLOCKED -> 0xFF8A8A;
            default -> 0x9CCBFF;
        };
    }
}
