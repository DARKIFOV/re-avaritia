from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/MysticalInfusionAssemblerBlockEntity.java"
text = PATH.read_text(encoding="utf-8")
old = '''        if (!canAcceptAe2Plan() || requestedOutput.isEmpty()\n                || !ItemStack.isSameItemSameTags(pattern.output(), requestedOutput)\n                || requestedAmount < pattern.output().getCount()) return false;\n'''
new = '''        if (!canAcceptAe2Plan() || requestedOutput.isEmpty()\n                || !ItemStack.isSameItemSameTags(pattern.output(), requestedOutput)\n                || requestedAmount < pattern.output().getCount()\n                || !canOutput(pattern.output())) return false;\n'''
if new not in text:
    if old not in text:
        raise RuntimeError("Mystical AE2 safety anchor not found")
    text = text.replace(old, new, 1)
PATH.write_text(text, encoding="utf-8")
print("Hardened Mystical infusion AE2 output validation")
