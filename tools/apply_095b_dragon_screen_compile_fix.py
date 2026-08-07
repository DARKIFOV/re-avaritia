from pathlib import Path

path = Path(__file__).resolve().parents[1] / "src/main/java/ru/rfvv/metatechreborn/client/screen/ExtremeDragonAssemblerScreen.java"
text = path.read_text(encoding="utf-8")
old = 'g.drawString(font, inventory.getDisplayName(), inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);'
new = 'g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF6D1414, false);'
if old in text:
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
elif new not in text:
    raise RuntimeError("0.6.95b screen compile fix: target line not found")
print("Applied 0.6.95b dragon screen inventory-label compile fix")
