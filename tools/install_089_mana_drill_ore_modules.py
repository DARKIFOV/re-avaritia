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

MODULES = {
    "mana_drill_module_ad_astra": {
        "modid": "ad_astra",
        "ru": "Рудный модуль мана-бура: Ad Astra",
        "en": "Mana Drill Ore Module: Ad Astra",
        "color": (82, 188, 255),
        "ingredients": ["ad_astra:raw_desh", "ad_astra:raw_ostrum", "ad_astra:raw_calorite", "ad_astra:ice_shard"],
        "mana_cost": 70000,
        "time": 220,
        "drops": [
            ("ad_astra:raw_desh", 2, 6, 8500),
            ("ad_astra:ice_shard", 2, 6, 8000),
            ("ad_astra:raw_ostrum", 1, 4, 6500),
            ("ad_astra:raw_calorite", 1, 3, 3500),
        ],
    },
    "mana_drill_module_thermal": {
        "modid": "thermal_foundation",
        "ru": "Рудный модуль мана-бура: Thermal",
        "en": "Mana Drill Ore Module: Thermal",
        "color": (255, 137, 55),
        "ingredients": ["thermal:raw_tin", "thermal:raw_lead", "thermal:raw_nickel", "thermal:raw_silver"],
        "mana_cost": 65000,
        "time": 210,
        "drops": [
            ("thermal:raw_tin", 2, 6, 9000),
            ("thermal:raw_lead", 2, 5, 8500),
            ("thermal:apatite", 2, 6, 7000),
            ("thermal:raw_nickel", 1, 4, 6500),
            ("thermal:raw_silver", 1, 4, 6000),
            ("thermal:niter", 2, 5, 5500),
            ("thermal:sulfur", 2, 5, 5500),
            ("thermal:cinnabar", 1, 3, 3500),
            ("thermal:ruby", 1, 2, 1800),
            ("thermal:sapphire", 1, 2, 1800),
        ],
    },
    "mana_drill_module_evolved_mekanism": {
        "modid": "evolvedmekanism",
        "ru": "Рудный модуль мана-бура: Evolved Mekanism",
        "en": "Mana Drill Ore Module: Evolved Mekanism",
        "color": (36, 216, 190),
        "ingredients": ["mekanism:raw_osmium", "mekanism:raw_uranium", "mekanism:fluorite_gem", "mekanism:raw_lead"],
        "mana_cost": 70000,
        "time": 220,
        "drops": [
            ("mekanism:raw_osmium", 2, 5, 8000),
            ("mekanism:raw_tin", 2, 5, 8000),
            ("mekanism:raw_lead", 2, 5, 7000),
            ("mekanism:fluorite_gem", 2, 6, 6500),
            ("mekanism:raw_uranium", 1, 4, 5000),
        ],
    },
    "mana_drill_module_mekanism_extras": {
        "modid": "mekanism_extras",
        "ru": "Рудный модуль мана-бура: Mekanism Extras",
        "en": "Mana Drill Ore Module: Mekanism Extras",
        "color": (175, 78, 255),
        "ingredients": ["mekanism_extras:raw_naquadah"],
        "mana_cost": 100000,
        "time": 260,
        "drops": [
            ("mekanism_extras:raw_naquadah", 1, 2, 3500),
        ],
    },
    "mana_drill_module_powah": {
        "modid": "powah",
        "ru": "Рудный модуль мана-бура: Powah",
        "en": "Mana Drill Ore Module: Powah",
        "color": (255, 220, 66),
        "ingredients": ["powah:uraninite_raw"],
        "mana_cost": 65000,
        "time": 200,
        "drops": [
            ("powah:uraninite_raw", 2, 6, 8000),
        ],
    },
    "mana_drill_module_mythicbotany": {
        "modid": "mythicbotany",
        "ru": "Рудный модуль мана-бура: MythicBotany",
        "en": "Mana Drill Ore Module: MythicBotany",
        "color": (225, 90, 222),
        "ingredients": ["mythicbotany:raw_elementium", "botania:dragonstone"],
        "mana_cost": 90000,
        "time": 260,
        "drops": [
            ("mythicbotany:raw_elementium", 1, 3, 4500),
            ("botania:dragonstone", 1, 2, 2500),
        ],
    },
    "mana_drill_module_mystical_agriculture": {
        "modid": "mysticalagriculture",
        "ru": "Рудный модуль мана-бура: Mystical Agriculture",
        "en": "Mana Drill Ore Module: Mystical Agriculture",
        "color": (91, 218, 79),
        "ingredients": ["mysticalagriculture:inferium_essence", "mysticalagriculture:prosperity_shard", "mysticalagriculture:soulium_dust"],
        "mana_cost": 65000,
        "time": 200,
        "drops": [
            ("mysticalagriculture:inferium_essence", 4, 12, 10000),
            ("mysticalagriculture:prosperity_shard", 2, 6, 7500),
            ("mysticalagriculture:soulium_dust", 1, 4, 4000),
        ],
    },
}


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def make_module_png(path: Path, accent: tuple[int, int, int]) -> None:
    width = height = 32
    pixels = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]

    def set_px(x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
        if 0 <= x < width and 0 <= y < height:
            pixels[y][x] = rgba

    def rect(x0: int, y0: int, x1: int, y1: int, rgba: tuple[int, int, int, int]) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                set_px(x, y, rgba)

    gold = (210, 154, 46, 255)
    bright_gold = (255, 216, 92, 255)
    dark = (23, 28, 34, 255)
    metal = (75, 83, 91, 255)
    accent_dark = tuple(max(0, c // 2) for c in accent) + (255,)
    accent_rgba = accent + (255,)
    accent_light = tuple(min(255, c + 70) for c in accent) + (255,)

    # Compact techno-card frame.
    rect(3, 3, 28, 28, dark)
    rect(4, 4, 27, 4, gold)
    rect(4, 27, 27, 27, gold)
    rect(4, 5, 4, 26, gold)
    rect(27, 5, 27, 26, gold)
    rect(6, 6, 25, 25, (35, 43, 50, 255))
    rect(7, 7, 24, 24, metal)
    rect(8, 8, 23, 23, (19, 24, 30, 255))

    # Central faceted ore/crystal.
    for y in range(9, 23):
        half = 7 - abs(15 - y)
        for x in range(16 - max(1, half), 16 + max(1, half) + 1):
            set_px(x, y, accent_rgba)
    for y in range(10, 17):
        for x in range(13, 17):
            if pixels[y][x][3] != 0:
                set_px(x, y, accent_light)
    for y in range(17, 22):
        for x in range(16, 21):
            if pixels[y][x][3] != 0:
                set_px(x, y, accent_dark)

    # Mana-blue spark and corner fasteners.
    for x, y in ((6, 6), (25, 6), (6, 25), (25, 25)):
        set_px(x, y, bright_gold)
    for x, y in ((10, 15), (22, 15), (16, 8), (16, 24)):
        set_px(x, y, (82, 230, 255, 255))

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


def mod_condition(modid: str) -> list[dict[str, str]]:
    return [{"type": "forge:mod_loaded", "modid": modid}]


def install_default_quartz_recipe() -> None:
    # Keep the existing vanilla resource pool and add Nether quartz to the standard module.
    write_json(
        DATA / "recipes/overworld_ore_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "module": {"item": "metatech_reborn:mana_drill_module"},
            "mana_cost": 50000,
            "time": 200,
            "drops": [
                {"item": "minecraft:raw_iron", "min": 2, "max": 6, "chance": 10000},
                {"item": "minecraft:raw_copper", "min": 3, "max": 9, "chance": 9000},
                {"item": "minecraft:raw_gold", "min": 1, "max": 4, "chance": 6500},
                {"item": "minecraft:redstone", "min": 4, "max": 12, "chance": 7000},
                {"item": "minecraft:lapis_lazuli", "min": 3, "max": 9, "chance": 5000},
                {"item": "minecraft:quartz", "min": 2, "max": 8, "chance": 4500},
                {"item": "minecraft:diamond", "min": 1, "max": 2, "chance": 900},
                {"item": "minecraft:emerald", "min": 1, "max": 2, "chance": 500},
            ],
        },
    )


def install_module(name: str, spec: dict[str, object]) -> None:
    modid = str(spec["modid"])
    make_module_png(ASSETS / f"textures/item/{name}.png", spec["color"])
    write_json(
        ASSETS / f"models/item/{name}.json",
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"metatech_reborn:item/{name}"}},
    )

    ingredients = [{"item": "metatech_reborn:mana_drill_module"}]
    ingredients.extend({"item": item} for item in spec["ingredients"])
    write_json(
        DATA / f"recipes/{name}.json",
        {
            "type": "minecraft:crafting_shapeless",
            "conditions": mod_condition(modid),
            "ingredients": ingredients,
            "result": {"item": f"metatech_reborn:{name}", "count": 1},
        },
    )

    drops = [
        {"item": item, "min": minimum, "max": maximum, "chance": chance}
        for item, minimum, maximum, chance in spec["drops"]
    ]
    write_json(
        DATA / f"recipes/{name}_drilling.json",
        {
            "type": "metatech_reborn:mana_drill_generating",
            "conditions": mod_condition(modid),
            "module": {"item": f"metatech_reborn:{name}"},
            "mana_cost": spec["mana_cost"],
            "time": spec["time"],
            "drops": drops,
        },
    )


def main() -> None:
    install_default_quartz_recipe()
    ru: dict[str, str] = {}
    en: dict[str, str] = {}
    for name, spec in MODULES.items():
        install_module(name, spec)
        ru[f"item.metatech_reborn.{name}"] = str(spec["ru"])
        en[f"item.metatech_reborn.{name}"] = str(spec["en"])
    patch_lang("ru_ru", ru)
    patch_lang("en_us", en)
    print("Installed 0.6.89 mana drill ore modules, recipes, icons and quartz support")


if __name__ == "__main__":
    main()
