package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Shared vanilla-like Minecraft GUI pieces. */
final class MetaTechGui {
    static final int BG = 0xFFC6C6C6;
    static final int PANEL = 0xFFD8D8D8;
    static final int PANEL_DARK = 0xFFB5B5B5;
    static final int BORDER = 0xFF555555;
    static final int SLOT = 0xFF8B8B8B;
    static final int SLOT_BORDER = 0xFF373737;
    static final int CYAN = 0xFF3A86B8;
    static final int PURPLE = 0xFF7653A6;
    static final int GOLD = 0xFFD89B2B;

    private MetaTechGui() {}

    static void background(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF373737);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, BG);
    }

    static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF555555);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
    }

    static void slot(GuiGraphics g, int x, int y, int accent) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + 15, y + 15, 0xFFB8B8B8);
        if ((accent >>> 24) != 0) g.fill(x + 1, y + 15, x + 15, y + 16, accent);
    }

    static void grid(GuiGraphics g, int x, int y, int columns, int rows, int accent) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                slot(g, x + column * 18, y + row * 18, accent);
            }
        }
    }

    static int drawWrapped(GuiGraphics g, Font font, Component text,
                           int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int index = 0; index < count; index++) {
            g.drawString(font, lines.get(index), x, y + index * 10, color, false);
        }
        return count * 10;
    }
}
