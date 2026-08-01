package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.ExtremePatternEncoderBlockEntity;
import ru.rfvv.metatechreborn.menu.ExtremePatternEncoderMenu;

public final class ExtremePatternEncoderScreen extends AbstractContainerScreen<ExtremePatternEncoderMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/molecular_assembler_9x9.png");

    public ExtremePatternEncoderScreen(ExtremePatternEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 256;
        inventoryLabelX = 8;
        inventoryLabelY = 168;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.encode_pattern"),
                        button -> sendButton(ExtremePatternEncoderMenu.ENCODE_BUTTON_ID))
                .bounds(leftPos + 174, topPos + 17, 74, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.metatech_reborn.clear_pattern_grid"),
                        button -> sendButton(ExtremePatternEncoderMenu.CLEAR_BUTTON_ID))
                .bounds(leftPos + 174, topPos + 126, 74, 18).build());
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 512);
        graphics.fill(leftPos + 173, topPos + 49, leftPos + 249, topPos + 119, 0xAA07171D);
        graphics.fill(leftPos + 189, topPos + 53, leftPos + 208, topPos + 73, 0xFF29434B);
        graphics.fill(leftPos + 189, topPos + 91, leftPos + 208, topPos + 111, 0xFF29434B);
        graphics.fill(leftPos + 214, topPos + 80, leftPos + 240, topPos + 84,
                menu.hasValidRecipe() ? 0xFF4EE08A : 0xFFFF4F67);
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 4, 0xEAF8FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.blank_pattern"),
                174, 43, 0xBBD5E7, false);
        graphics.drawString(font, Component.translatable("gui.metatech_reborn.encoded_pattern"),
                174, 111, 0xBBD5E7, false);
        graphics.drawString(font, statusText(menu.getStatus()), 174, 149, statusColor(menu.getStatus()), false);
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
