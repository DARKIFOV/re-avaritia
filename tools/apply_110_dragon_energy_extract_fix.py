from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"
text = path.read_text(encoding="utf-8")

old = "new EnergyStorage(ENERGY_CAPACITY, 100_000_000, 0)"
new = "new EnergyStorage(ENERGY_CAPACITY, 100_000_000, 100_000_000)"

if new not in text:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one Dragon Assembler EnergyStorage constructor, found {count}")
    text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")
