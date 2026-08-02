package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.LuckConverterBlockEntity;
import ru.rfvv.metatechreborn.menu.LuckConverterMenu;

public final class LuckConverterScreen extends AbstractContainerScreen<LuckConverterMenu> {
    private static final ResourceLocation BASIC_BACKGROUND = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/luck_converter.png");
    private static final ResourceLocation ADVANCED_BACKGROUND = new ResourceLocation(
            MetaTechReborn.MOD_ID, "textures/gui/advanced_luck_converter.png");

    public LuckConverterScreen(LuckConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.isAdvanced() ? 368 : 332;
        imageHeight = menu.isAdvanced() ? 404 : 306;
        inventoryLabelX = menu.isAdvanced() ? 101 : 83;
        inventoryLabelY = menu.isAdvanced() ? 310 : 210;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick,
                            int mouseX, int mouseY) {
        ResourceLocation texture = menu.isAdvanced()
                ? ADVANCED_BACKGROUND : BASIC_BACKGROUND;
        g.blit(texture, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 512, 512);

        int columns = menu.isAdvanced() ? 12 : 10;
        int inputRows = menu.isAdvanced() ? 6 : 3;
        int outputRows = menu.isAdvanced() ? 5 : 3;
        int outputY = menu.isAdvanced() ? 150 : 96;
        int statusY = menu.isAdvanced() ? 250 : 158;
        int playerX = menu.isAdvanced() ? 101 : 83;
        int playerY = menu.isAdvanced() ? 322 : 222;
        int upgradesX = menu.isAdvanced() ? 242 : 206;
        int utilityX = menu.isAdvanced() ? 316 : 280;
        int gridWidth = columns * 18;

        MetaTechGui.grid(g, leftPos + 10, topPos + 30,
                columns, inputRows, 0xFF42CAE8);
        MetaTechGui.grid(g, leftPos + 10, topPos + outputY,
                columns, outputRows, 0xFF7356D8);

        for (int index = 0; index < LuckConverterBlockEntity.UPGRADE_SLOTS; index++) {
            int column = index % 2;
            int row = index / 2;
            int accent = index < 3 ? 0xFF42CAE8 : 0xFFFFA43A;
            MetaTechGui.slot(g, leftPos + upgradesX + column * 24,
                    topPos + 42 + row * 26, accent);
        }
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 42, 0xFF55E58A);
        MetaTechGui.slot(g, leftPos + utilityX, topPos + 94, 0xFFFFCC45);

        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY,
                9, 3, 0xFF73879A);
        MetaTechGui.grid(g, leftPos + playerX, topPos + playerY + 58,
                9, 1, 0xFF73879A);

        int barX = leftPos + 10;
        int barY = topPos + statusY + 39;
        g.fill(barX, barY, barX + gridWidth, barY + 9, 0xFF03090D);
        g.fill(barX + 1, barY + 1,
                barX + 1 + menu.progressPixels(gridWidth - 2),
                barY + 8, MetaTechGui.CYAN);

        int energyHeight = 68;
        int energyPixels = menu.energyPixels(energyHeight);
        g.fill(leftPos + utilityX + 22, topPos + 42,
                leftPos + utilityX + 29, topPos + 42 + energyHeight, 0xFF03090D);
        g.fill(leftPos + utilityX + 23,
                topPos + 42 + energyHeight - energyPixels,
                leftPos + utilityX + 28,
                topPos + 42 + energyHeight, MetaTechGui.GOLD);

        int speedColor = menu.isInstantSpeed() ? 0xFFE783FF
                : menu.getSpeedBonusPercent() >= 70 ? 0xFF55E58A
                : menu.getSpeedBonusPercent() >= 30 ? 0xFF42CAE8
                : 0xFF53666D;
        g.fill(leftPos + upgradesX, topPos + 126,
                leftPos + utilityX + 18, topPos + 132, 0xFF03090D);
        int fullWidth = utilityX + 18 - upgradesX;
        int speedWidth = menu.isInstantSpeed()
                ? fullWidth
                : fullWidth * menu.getSpeedBonusPercent() / 100;
        g.fill(leftPos + upgradesX, topPos + 126,
                leftPos + upgradesX + speedWidth, topPos + 132, speedColor);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        int columns = menu.isAdvanced() ? 12 : 10;
        int outputY = menu.isAdvanced() ? 150 : 96;
        int statusY = menu.isAdvanced() ? 250 : 158;
        int gridWidth = columns * 18;
        int sideLabelX = menu.isAdvanced() ? 238 : 202;

        g.drawString(font, title, 10, 8, 0xEAF8FF, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.inputs"),
                10, 20, 0x9CCBFF, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.outputs"),
                10, outputY - 10, 0xC0A8FF, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.upgrades"),
                sideLabelX, 20, 0xFFD27A, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.speed_active"),
                sideLabelX, 116, 0x9CCBFF, false);
        g.drawString(font, speedText(), sideLabelX, 136,
                menu.isInstantSpeed() ? 0xE783FF : 0x6ED7FF, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.module"),
                menu.isAdvanced() ? 306 : 270, 30, 0x80F0A8, false);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.energy_slot"),
                menu.isAdvanced() ? 306 : 270, 82, 0xF4D27A, false);
        g.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, 0xBBD5E7, false);

        MetaTechGui.drawWrapped(g, font,
                Component.translatable(statusKey(menu.getStatus())),
                10, statusY + 5, gridWidth - 8,
                statusColor(menu.getStatus()), 2);
        g.drawString(font,
                Component.translatable("gui.metatech_reborn.luck_converter.stats",
                        menu.getLuckLevel(), menu.getOperations(), menu.getEnergyPerTick()),
                10, statusY + 24, 0x9CD8FF, false);
        g.drawString(font,
                Component.literal(menu.getEnergy() + " / "
                        + menu.getEnergyCapacity() + " FE"),
                10, statusY + 35, 0xF4D27A, false);
    }

    private Component speedText() {
        if (menu.isInstantSpeed()) {
            return Component.translatable(
                    "gui.metatech_reborn.luck_converter.speed.instant");
        }
        if (menu.getSpeedBonusPercent() <= 0) {
            return Component.translatable(
                    "gui.metatech_reborn.luck_converter.speed.none");
        }
        return Component.translatable(
                "gui.metatech_reborn.luck_converter.speed.percent",
                menu.getSpeedBonusPercent());
    }

    private static String statusKey(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING ->
                    "gui.metatech_reborn.luck_converter.status.running";
            case LuckConverterBlockEntity.STATUS_NO_MODULE ->
                    "gui.metatech_reborn.luck_converter.status.no_module";
            case LuckConverterBlockEntity.STATUS_NO_ENERGY ->
                    "gui.metatech_reborn.luck_converter.status.no_energy";
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL ->
                    "gui.metatech_reborn.luck_converter.status.output_full";
            case LuckConverterBlockEntity.STATUS_NO_VALID_INPUT ->
                    "gui.metatech_reborn.luck_converter.status.no_input";
            default -> "gui.metatech_reborn.luck_converter.status.idle";
        };
    }

    private static int statusColor(int status) {
        return switch (status) {
            case LuckConverterBlockEntity.STATUS_RUNNING -> 0x55E58A;
            case LuckConverterBlockEntity.STATUS_NO_ENERGY -> 0xF5CA59;
            case LuckConverterBlockEntity.STATUS_OUTPUT_FULL -> 0xFF9454;
            case LuckConverterBlockEntity.STATUS_NO_MODULE,
                 LuckConverterBlockEntity.STATUS_NO_VALID_INPUT -> 0xFF6477;
            default -> 0xA9BAC5;
        };
    }
}
