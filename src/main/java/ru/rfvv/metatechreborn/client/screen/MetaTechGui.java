package ru.rfvv.metatechreborn.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Shared code-drawn GUI pieces. These do not depend on a background PNG, so slots and labels stay aligned. */
final class MetaTechGui {
    static final int BG = 0xF008111A;
    static final int PANEL = 0xFF132431;
    static final int PANEL_DARK = 0xFF0A1720;
    static final int BORDER = 0xFF5E7485;
    static final int SLOT = 0xFF1B2A38;
    static final int SLOT_BORDER = 0xFF7890A3;
    static final int CYAN = 0xFF43D7F5;
    static final int PURPLE = 0xFF7A5DE8;
    static final int GOLD = 0xFFFFC857;

    private MetaTechGui() {}

    static void background(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, BG);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, BORDER);
        g.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFF0E1924);
    }

    static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, BORDER);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_DARK);
    }

    static void slot(GuiGraphics g, int x, int y, int accent) {
        g.fill(x - 1, y - 1, x + 17, y + 17, accent);
        g.fill(x, y, x + 16, y + 16, SLOT);
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
