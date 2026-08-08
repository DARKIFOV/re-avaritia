from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"
text = path.read_text(encoding="utf-8")
old = '''        energy.receiveEnergy(tag.getInt("Energy"), false);\n        progress = tag.getInt("Progress");\n'''
new = '''        int storedEnergy = Math.max(0, tag.getInt("Energy"));\n        while (storedEnergy > 0) {\n            int accepted = energy.receiveEnergy(Math.min(storedEnergy, 100_000_000), false);\n            if (accepted <= 0) break;\n            storedEnergy -= accepted;\n        }\n        progress = tag.getInt("Progress");\n'''
if new not in text:
    if text.count(old) != 1:
        raise RuntimeError("dragon assembler energy reload patch target not found")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Applied 0.6.94 full energy reload fix for Extreme Dragon Assembler")
