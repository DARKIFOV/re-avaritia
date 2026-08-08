from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"
text = path.read_text(encoding="utf-8")
old = "new EnergyStorage(ENERGY_CAPACITY, 100_000_000, 0)"
new = "new EnergyStorage(2_000_000_000, 100_000_000, 100_000_000)"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise RuntimeError("Extreme Dragon Assembler EnergyStorage constructor not found")
path.write_text(text, encoding="utf-8")
