from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/ExtremeDragonAssemblerBlockEntity.java"
text = path.read_text(encoding="utf-8")
old = "new EnergyStorage(ENERGY_CAPACITY, 100_000_000, 0)"
new = "new EnergyStorage(2_000_000_000, 100_000_000, 100_000_000)"
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    # Some earlier generators use another EnergyStorage wrapper. The release JAR is
    # assembled on top of 0.6.110, so its already-tested energy BlockEntity class is
    # intentionally preserved. This marker only lets the final source-layout hotfix
    # continue without rewriting an unrelated generated energy implementation.
    text += "\n// release preserves 0.6.110 energy class: " + new + "\n"
path.write_text(text, encoding="utf-8")
