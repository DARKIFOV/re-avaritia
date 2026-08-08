from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Luck converter: give the upgrade/status panel its own vertical space and move
# the player inventory down. This prevents status/bars from colliding with slots.
menu = ROOT / "src/main/java/ru/rfvv/metatechreborn/menu/LuckConverterMenu.java"
text = menu.read_text(encoding="utf-8")
text = text.replace("int playerY = advanced ? 222 : 188;", "int playerY = advanced ? 230 : 198;")
menu.write_text(text, encoding="utf-8")

screen = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/screen/LuckConverterScreen.java"
text = screen.read_text(encoding="utf-8")
text = text.replace("imageHeight = menu.isAdvanced() ? 318 : 270;", "imageHeight = menu.isAdvanced() ? 326 : 284;")
text = text.replace("inventoryLabelY = menu.isAdvanced() ? 210 : 176;", "inventoryLabelY = menu.isAdvanced() ? 218 : 186;")
text = text.replace("int playerY = advanced ? 222 : 188;", "int playerY = advanced ? 230 : 198;")
text = text.replace("MetaTechGui.panel(g, leftPos + sideX, topPos + 20, sideWidth, 148);",
                    "MetaTechGui.panel(g, leftPos + sideX, topPos + 20, sideWidth, 164);")
text = text.replace("drawBar(g, barX, 110, barW, menu.progressPixels(barW - 2), MetaTechGui.CYAN);",
                    "drawBar(g, barX, 150, barW, menu.progressPixels(barW - 2), MetaTechGui.CYAN);")
text = text.replace("drawBar(g, barX, 128, barW, menu.energyPixels(barW - 2), MetaTechGui.GOLD);",
                    "drawBar(g, barX, 164, barW, menu.energyPixels(barW - 2), MetaTechGui.GOLD);")
text = text.replace("drawBar(g, barX, 92, barW, speed, menu.isInstantSpeed() ? 0xFF8E44AD : 0xFF3A9D72);",
                    "drawBar(g, barX, 136, barW, speed, menu.isInstantSpeed() ? 0xFF8E44AD : 0xFF3A9D72);")
text = text.replace("sideX + 8, 70, 144, statusColor(menu.getStatus()), 2);",
                    "sideX + 8, 112, 144, statusColor(menu.getStatus()), 2);")
text = text.replace('        g.drawString(font, Component.literal("×" + menu.getOperations() + "  L" + menu.getLuckLevel()),\n                sideX + 8, 146, 0x255C88, false);\n', '')
text = text.replace("sideX + 8, 110, barW, 8", "sideX + 8, 150, barW, 8")
text = text.replace("sideX + 8, 128, barW, 8", "sideX + 8, 164, barW, 8")
text = text.replace("sideX + 8, 92, barW, 8", "sideX + 8, 136, barW, 8")
screen.write_text(text, encoding="utf-8")

# Dragon assembler: tier line starts below the final 4x3 ingredient row.
assembler = ROOT / "src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java"
text = assembler.read_text(encoding="utf-8")
text = text.replace("                66, 94, 140, tier < 0 ? 0xFFFF6868 : 0xFFFFB0A6, 2);",
                    "                66, 100, 140, tier < 0 ? 0xFFFF6868 : 0xFFFFB0A6, 2);")
assembler.write_text(text, encoding="utf-8")
