from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/ru/rfvv/metatechreborn/blockentity/GreenhouseBlockEntity.java"
text = PATH.read_text(encoding="utf-8")
old = "    public static final int MANA_CAPACITY = 2_000_000;"
new = "    public static final int MANA_CAPACITY = 128_000_000;"
if new not in text:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"greenhouse mana capacity: expected one match, found {count}")
    text = text.replace(old, new, 1)
PATH.write_text(text, encoding="utf-8")
print("Set greenhouse mana capacity to 128,000,000")
