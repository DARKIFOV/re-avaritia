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

OMNI = "mana_drill_module_omni"
MOD_MODULES = [
    ("mana_drill_module_ad_astra", "ad_astra"),
    ("mana_drill_module_thermal", "thermal_foundation"),
    ("mana_drill_module_evolved_mekanism", "evolvedmekanism"),
    ("mana_drill_module_mekanism_extras", "mekanism_extras"),
    ("mana_drill_module_powah", "powah"),
    ("mana_drill_module_mythicbotany", "mythicbotany"),
    ("mana_drill_module_mystical_agriculture", "mysticalagriculture"),
]


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def make_omni_png(path: Path) -> None:
    width = height = 32
    pixels = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def set_px(x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
        if 0 <= x < width and 0 <= y < height:
            pixels[y][x] = rgba

    def rect(x0: int, y0: int, x1: int, y1: int, rgba: tuple[int, int, int, int]) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                set_px(x, y, rgba)

    dark = (18, 22, 30, 255)
    metal = (67, 76, 88, 255)
    gold = (236, 177, 54, 255)
    white = (230, 249, 255, 255)
    cyan = (82, 230, 255, 255)
    colors = [
        (82, 188, 255, 255),
        (255, 137, 55, 255),
        (36, 216, 190, 255),
        (175, 78, 255, 255),
        (255, 220, 66, 255),
        (225, 90, 222, 255),
        (91, 218, 79, 255),
        (220, 220, 230, 255),
    ]

    rect(2, 2, 29, 29, dark)
    rect(3, 3, 28, 3, gold)
    rect(3, 28, 28, 28, gold)
    rect(3, 4, 3, 27, gold)
    rect(28, 4, 28, 27, gold)
    rect(5, 5, 26, 26, metal)
    rect(6, 6, 25, 25, dark)

    # Eight colored facets around a deliberately empty dark center.
    facets = [
        (11, 7), (16, 7), (21, 11), (21, 17),
        (16, 22), (11, 22), (7, 17), (7, 11),
    ]
    for (cx, cy), color in zip(facets, colors):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 3:
                    set_px(cx + dx, cy + dy, color)
        set_px(cx - 1, cy - 1, white)

    # Empty central socket mirrors the empty centre of the 3x3 crafting recipe.
    rect(13, 13, 18, 18, (5, 8, 13, 255))
    rect(14, 14, 17, 17, (17, 23, 31, 255))
    for x, y in ((15, 5), (26, 15), (15, 26), (5, 15)):
        set_px(x, y, cyan)

    rows: list[bytes] = []
    for row_pixels in pixels:
        row = bytearray([0])
        for rgba in row_pixels:
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


def all_mod_conditions() -> list[dict[str, str]]:
    return [{"type": "forge:mod_loaded", "modid": modid} for _, modid in MOD_MODULES]


def load_drill_recipe(name: str) -> dict[str, object]:
    path = DATA / f"recipes/{name}.json"
    if not path.exists():
        raise RuntimeError(f"Required generated mana-drill recipe is missing: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    make_omni_png(ASSETS / f"textures/item/{OMNI}.png")
    write_json(
        ASSETS / f"models/item/{OMNI}.json",
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"metatech_reborn:item/{OMNI}"}},
    )

    # Exactly eight modules around the edge of a 3x3 crafting grid; the centre is intentionally empty.
    write_json(
        DATA / f"recipes/{OMNI}.json",
        {
            "type": "minecraft:crafting_shaped",
            "conditions": all_mod_conditions(),
            "pattern": ["ABC", "D E", "FGH"],
            "key": {
                "A": {"item": "metatech_reborn:mana_drill_module"},
                "B": {"item": "metatech_reborn:mana_drill_module_ad_astra"},
                "C": {"item": "metatech_reborn:mana_drill_module_thermal"},
                "D": {"item": "metatech_reborn:mana_drill_module_evolved_mekanism"},
                "E": {"item": "metatech_reborn:mana_drill_module_mekanism_extras"},
                "F": {"item": "metatech_reborn:mana_drill_module_powah"},
                "G": {"item": "metatech_reborn:mana_drill_module_mythicbotany"},
                "H": {"item": "metatech_reborn:mana_drill_module_mystical_agriculture"},
            },
            "result": {"item": f"metatech_reborn:{OMNI}", "count": 1},
        },
    )

    # Combine the exact output pools of the standard module plus every optional ore module.
    source_recipes = [load_drill_recipe("overworld_ore_drilling")]
    source_recipes.extend(load_drill_recipe(f"{name}_drilling") for name, _ in MOD_MODULES)
    drops: list[dict[str, object]] = []
    for recipe in source_recipes:
        drops.extend(recipe.get("drops", []))

    # Cost equals running all eight source modules once. Cycle time is the slowest source module.
    mana_cost = sum(int(recipe.get("mana_cost", 0)) for recipe in source_recipes)
    time = max(int(recipe.get("time", 1)) for recipe in source_recipes)

    write_json(
        DATA / f"recipes/{OMNI}_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "conditions": all_mod_conditions(),
            "module": {"item": f"metatech_reborn:{OMNI}"},
            "mana_cost": mana_cost,
            "time": time,
            "drops": drops,
        },
    )

    patch_lang(
        "ru_ru",
        {
            f"item.metatech_reborn.{OMNI}": "Омни-модуль мана-бура",
            f"tooltip.metatech_reborn.{OMNI}": "Объединяет обычный и все установленные рудные модули",
        },
    )
    patch_lang(
        "en_us",
        {
            f"item.metatech_reborn.{OMNI}": "Mana Drill Omni Module",
            f"tooltip.metatech_reborn.{OMNI}": "Combines the standard module with every supported ore module",
        },
    )
    print(f"Installed 0.6.90 omni mana-drill module: {len(drops)} drop entries, {mana_cost} mana, {time} ticks")


if __name__ == "__main__":
    main()
