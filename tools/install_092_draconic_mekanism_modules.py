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

MEKANISM = "mana_drill_module_mekanism"
DRACONIC = "mana_drill_module_draconic_evolution"
OMNI = "mana_drill_module_omni"


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def make_dual_module_png(path: Path, left: tuple[int, int, int], right: tuple[int, int, int]) -> None:
    width = height = 32
    pixels = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def set_px(x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
        if 0 <= x < width and 0 <= y < height:
            pixels[y][x] = rgba

    def rect(x0: int, y0: int, x1: int, y1: int, rgba: tuple[int, int, int, int]) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                set_px(x, y, rgba)

    dark = (16, 20, 27, 255)
    metal = (68, 78, 91, 255)
    gold = (232, 173, 49, 255)
    white = (239, 252, 255, 255)
    rect(2, 2, 29, 29, dark)
    rect(3, 3, 28, 3, gold)
    rect(3, 28, 28, 28, gold)
    rect(3, 4, 3, 27, gold)
    rect(28, 4, 28, 27, gold)
    rect(5, 5, 26, 26, metal)
    rect(6, 6, 25, 25, dark)

    for y in range(7, 25):
        half = max(1, 8 - abs(15 - y))
        for x in range(16 - half, 16 + half + 1):
            color = left if x < 16 else right
            shade = 1.0 if y < 16 else 0.65
            set_px(x, y, tuple(int(c * shade) for c in color) + (255,))
    for x, y in ((12, 10), (19, 10), (10, 16), (21, 16), (15, 8), (16, 22)):
        set_px(x, y, white)

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


def load_recipe(name: str) -> dict[str, object]:
    path = DATA / f"recipes/{name}.json"
    if not path.exists():
        raise RuntimeError(f"Required generated recipe is missing: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    # Keep the two legacy Mekanism item IDs registered for world compatibility, but remove their new crafting recipes.
    for legacy in ("mana_drill_module_evolved_mekanism", "mana_drill_module_mekanism_extras"):
        craft = DATA / f"recipes/{legacy}.json"
        if craft.exists():
            craft.unlink()

    make_dual_module_png(ASSETS / f"textures/item/{MEKANISM}.png", (35, 220, 205), (177, 80, 255))
    make_dual_module_png(ASSETS / f"textures/item/{DRACONIC}.png", (155, 55, 245), (255, 118, 32))
    for name in (MEKANISM, DRACONIC):
        write_json(
            ASSETS / f"models/item/{name}.json",
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"metatech_reborn:item/{name}"}},
        )

    mekanism_conditions = [
        {"type": "forge:mod_loaded", "modid": "evolvedmekanism"},
        {"type": "forge:mod_loaded", "modid": "mekanism_extras"},
    ]
    write_json(
        DATA / f"recipes/{MEKANISM}.json",
        {
            "type": "minecraft:crafting_shapeless",
            "conditions": mekanism_conditions,
            "ingredients": [
                {"item": "metatech_reborn:mana_drill_module"},
                {"item": "mekanism:raw_osmium"},
                {"item": "mekanism:raw_uranium"},
                {"item": "mekanism:fluorite_gem"},
                {"item": "mekanism_extras:raw_naquadah"},
            ],
            "result": {"item": f"metatech_reborn:{MEKANISM}", "count": 1},
        },
    )
    write_json(
        DATA / f"recipes/{MEKANISM}_from_legacy.json",
        {
            "type": "minecraft:crafting_shapeless",
            "conditions": mekanism_conditions,
            "ingredients": [
                {"item": "metatech_reborn:mana_drill_module_evolved_mekanism"},
                {"item": "metatech_reborn:mana_drill_module_mekanism_extras"},
            ],
            "result": {"item": f"metatech_reborn:{MEKANISM}", "count": 1},
        },
    )

    evolved = load_recipe("mana_drill_module_evolved_mekanism_drilling")
    extras = load_recipe("mana_drill_module_mekanism_extras_drilling")
    mekanism_drops = list(evolved.get("drops", [])) + list(extras.get("drops", []))
    write_json(
        DATA / f"recipes/{MEKANISM}_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "conditions": mekanism_conditions,
            "module": {"item": f"metatech_reborn:{MEKANISM}"},
            "mana_cost": int(evolved.get("mana_cost", 0)) + int(extras.get("mana_cost", 0)),
            "time": max(int(evolved.get("time", 1)), int(extras.get("time", 1))),
            "drops": mekanism_drops,
        },
    )

    draconic_conditions = [{"type": "forge:mod_loaded", "modid": "draconicevolution"}]
    write_json(
        DATA / f"recipes/{DRACONIC}.json",
        {
            "type": "minecraft:crafting_shapeless",
            "conditions": draconic_conditions,
            "ingredients": [
                {"item": "metatech_reborn:mana_drill_module"},
                {"item": "draconicevolution:draconium_dust"},
                {"item": "draconicevolution:draconium_dust"},
                {"item": "draconicevolution:draconium_dust"},
                {"item": "draconicevolution:draconium_dust"},
            ],
            "result": {"item": f"metatech_reborn:{DRACONIC}", "count": 1},
        },
    )
    write_json(
        DATA / f"recipes/{DRACONIC}_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "conditions": draconic_conditions,
            "module": {"item": f"metatech_reborn:{DRACONIC}"},
            "mana_cost": 85000,
            "time": 230,
            "drops": [
                {"item": "draconicevolution:draconium_dust", "min": 2, "max": 8, "chance": 9000}
            ],
        },
    )

    # Rebuild the Omni ring: base + Ad Astra + Thermal + merged Mekanism + Draconic + Powah + MythicBotany + Mystical Agriculture.
    omni_conditions = [
        {"type": "forge:mod_loaded", "modid": "ad_astra"},
        {"type": "forge:mod_loaded", "modid": "thermal_foundation"},
        {"type": "forge:mod_loaded", "modid": "evolvedmekanism"},
        {"type": "forge:mod_loaded", "modid": "mekanism_extras"},
        {"type": "forge:mod_loaded", "modid": "powah"},
        {"type": "forge:mod_loaded", "modid": "mythicbotany"},
        {"type": "forge:mod_loaded", "modid": "mysticalagriculture"},
        {"type": "forge:mod_loaded", "modid": "draconicevolution"},
    ]
    write_json(
        DATA / f"recipes/{OMNI}.json",
        {
            "type": "minecraft:crafting_shaped",
            "conditions": omni_conditions,
            "pattern": ["ABC", "D E", "FGH"],
            "key": {
                "A": {"item": "metatech_reborn:mana_drill_module"},
                "B": {"item": "metatech_reborn:mana_drill_module_ad_astra"},
                "C": {"item": "metatech_reborn:mana_drill_module_thermal"},
                "D": {"item": f"metatech_reborn:{MEKANISM}"},
                "E": {"item": f"metatech_reborn:{DRACONIC}"},
                "F": {"item": "metatech_reborn:mana_drill_module_powah"},
                "G": {"item": "metatech_reborn:mana_drill_module_mythicbotany"},
                "H": {"item": "metatech_reborn:mana_drill_module_mystical_agriculture"},
            },
            "result": {"item": f"metatech_reborn:{OMNI}", "count": 1},
        },
    )

    source_names = [
        "overworld_ore_drilling",
        "mana_drill_module_ad_astra_drilling",
        "mana_drill_module_thermal_drilling",
        f"{MEKANISM}_drilling",
        f"{DRACONIC}_drilling",
        "mana_drill_module_powah_drilling",
        "mana_drill_module_mythicbotany_drilling",
        "mana_drill_module_mystical_agriculture_drilling",
    ]
    source_recipes = [load_recipe(name) for name in source_names]
    drops: list[dict[str, object]] = []
    for recipe in source_recipes:
        drops.extend(recipe.get("drops", []))
    write_json(
        DATA / f"recipes/{OMNI}_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "conditions": omni_conditions,
            "module": {"item": f"metatech_reborn:{OMNI}"},
            "mana_cost": sum(int(recipe.get("mana_cost", 0)) for recipe in source_recipes),
            "time": max(int(recipe.get("time", 1)) for recipe in source_recipes),
            "drops": drops,
        },
    )

    patch_lang(
        "ru_ru",
        {
            f"item.metatech_reborn.{MEKANISM}": "Рудный модуль мана-бура: Mekanism",
            f"item.metatech_reborn.{DRACONIC}": "Рудный модуль мана-бура: Draconic Evolution",
            f"tooltip.metatech_reborn.{MEKANISM}": "Объединяет руды Evolved Mekanism и Mekanism Extras",
            f"tooltip.metatech_reborn.{DRACONIC}": "Добывает дракониевую пыль из Draconic Evolution",
        },
    )
    patch_lang(
        "en_us",
        {
            f"item.metatech_reborn.{MEKANISM}": "Mana Drill Ore Module: Mekanism",
            f"item.metatech_reborn.{DRACONIC}": "Mana Drill Ore Module: Draconic Evolution",
            f"tooltip.metatech_reborn.{MEKANISM}": "Combines Evolved Mekanism and Mekanism Extras ore pools",
            f"tooltip.metatech_reborn.{DRACONIC}": "Extracts Draconium Dust from Draconic Evolution",
        },
    )
    print(f"Installed 0.6.92 merged Mekanism and Draconic Evolution modules; Omni drops={len(drops)}")


if __name__ == "__main__":
    main()
