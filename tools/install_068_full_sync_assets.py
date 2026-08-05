from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATED = ROOT / "build/generated/metatech_assets"
ASSETS = GENERATED / "assets/metatech_reborn"
DATA = GENERATED / "data/metatech_reborn"
SOURCE_ASSETS = ROOT / "src/main/resources/assets/metatech_reborn"


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def make_water_module_png(path: Path) -> None:
    width = height = 16
    rows: list[bytes] = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            # Opaque Minecraft-style icon: dark wood frame, metal inner rim and blue water core.
            if x in (0, 15) or y in (0, 15):
                rgba = (74, 45, 24, 255)
            elif x in (1, 14) or y in (1, 14):
                rgba = (151, 94, 44, 255)
            elif x in (2, 13) or y in (2, 13):
                rgba = (198, 173, 109, 255)
            else:
                dx = x - 7.5
                dy = y - 8.0
                inside_drop = (dx * dx * 0.95 + (dy + 1.0) * (dy + 1.0) * 0.55 < 20.0 and y >= 4) or (abs(dx) <= max(0.0, (y - 2) * 0.45) and 2 <= y <= 8)
                if inside_drop:
                    highlight = x <= 6 and y <= 8
                    rgba = (92, 206, 255, 255) if highlight else (31, 128, 223, 255)
                else:
                    rgba = (44, 63, 67, 255)
            row.extend(rgba)
        rows.append(bytes(row))
    raw = b"".join(rows)
    png = b"\x89PNG\r\n\x1a\n"
    png += png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += png_chunk(b"IDAT", zlib.compress(raw, 9))
    png += png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def patch_lang(locale: str, additions: dict[str, str]) -> None:
    source = SOURCE_ASSETS / "lang" / f"{locale}.json"
    data: dict[str, str] = {}
    if source.exists():
        data.update(json.loads(source.read_text(encoding="utf-8")))
    generated = ASSETS / "lang" / f"{locale}.json"
    if generated.exists():
        data.update(json.loads(generated.read_text(encoding="utf-8")))
    data.update(additions)
    write_json(generated, data)


def main() -> None:
    write_json(
        ASSETS / "models/item/greenhouse_infinite_water_module.json",
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "metatech_reborn:item/greenhouse_infinite_water_module"},
        },
    )
    make_water_module_png(ASSETS / "textures/item/greenhouse_infinite_water_module.png")
    write_json(
        DATA / "recipes/greenhouse_infinite_water_module.json",
        {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [
                {"item": "minecraft:water_bucket"},
                {"item": "botania:mana_diamond"},
                {"item": "minecraft:gold_ingot"},
            ],
            "result": {"item": "metatech_reborn:greenhouse_infinite_water_module", "count": 1},
        },
    )
    patch_lang(
        "ru_ru",
        {
            "item.metatech_reborn.greenhouse_infinite_water_module": "Модуль бесконечной воды",
            "tooltip.metatech_reborn.greenhouse_module.infinite_water": "Не расходует воду при работе теплицы",
            "gui.metatech_reborn.greenhouse.flower_stack": "До 16 одинаковых цветков работают одновременно",
        },
    )
    patch_lang(
        "en_us",
        {
            "item.metatech_reborn.greenhouse_infinite_water_module": "Infinite Water Module",
            "tooltip.metatech_reborn.greenhouse_module.infinite_water": "Prevents greenhouse water consumption",
            "gui.metatech_reborn.greenhouse.flower_stack": "Up to 16 identical flowers run in parallel",
        },
    )
    print("Installed 0.6.68 full-sync models, recipe, texture and localization")


if __name__ == "__main__":
    main()
