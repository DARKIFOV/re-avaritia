from __future__ import annotations

import base64
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "build/generated/metatech_assets/assets/metatech_reborn"
PARTS = ROOT / "tools/final_077_palette"
NAMES = ['block/advanced_luck_converter.png', 'block/advanced_luck_converter_front.png', 'block/advanced_luck_converter_side.png', 'block/advanced_luck_converter_top.png', 'block/advanced_luck_converter_back.png', 'block/advanced_luck_converter_bottom.png', 'block/luck_converter.png', 'block/luck_converter_front.png', 'block/luck_converter_side.png', 'block/luck_converter_top.png', 'block/luck_converter_back.png', 'block/luck_converter_bottom.png', 'block/extreme_pattern_encoder.png', 'block/extreme_pattern_encoder_front.png', 'block/extreme_pattern_encoder_side.png', 'block/extreme_pattern_encoder_top.png', 'block/greenhouse_front.png', 'block/greenhouse_side.png', 'block/greenhouse_top.png', 'block/greenhouse_bottom.png', 'block/greenhouse_glass.png', 'block/greenhouse_mana_port.png', 'block/neutronium_combiner.png', 'block/neutronium_combiner_front.png', 'block/neutronium_combiner_side.png', 'block/neutronium_combiner_top.png', 'item/blank_extreme_pattern.png', 'item/encoded_extreme_pattern.png', 'item/greenhouse_speed_module.png', 'item/greenhouse_speed_module_2.png', 'item/greenhouse_speed_module_3.png', 'item/greenhouse_efficiency_module.png', 'item/greenhouse_efficiency_module_2.png', 'item/greenhouse_efficiency_module_3.png', 'item/greenhouse_economy_module.png', 'item/greenhouse_economy_module_2.png', 'item/greenhouse_economy_module_3.png', 'item/greenhouse_infinite_day_module.png', 'item/greenhouse_infinite_night_module.png', 'item/greenhouse_infinite_water_module.png', 'item/greenhouse_infinite_lava_module.png', 'item/neutron_combiner_speed_upgrade.png', 'item/neutron_combiner_efficiency_upgrade.png', 'item/neutron_combiner_output_upgrade.png']


def chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def write_png(path: Path, rgba: bytes) -> None:
    rows = b"".join(b"\x00" + rgba[y * 64:(y + 1) * 64] for y in range(16))
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(rows, 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def write_json(path: Path, data: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    encoded = "".join(path.read_text(encoding="ascii").strip() for path in sorted(PARTS.glob("part*.txt")))
    payload = zlib.decompress(base64.b85decode(encoded.encode("ascii")))
    palette = [tuple(payload[i:i + 4]) for i in range(0, 256, 4)]
    pixels = payload[256:]
    if len(pixels) != len(NAMES) * 256:
        raise RuntimeError("Invalid final 0.6.77 texture payload")
    for number, name in enumerate(NAMES):
        indices = pixels[number * 256:(number + 1) * 256]
        rgba = bytes(channel for index in indices for channel in palette[index])
        write_png(ASSETS / "textures" / name, rgba)

    for machine in ("luck_converter", "advanced_luck_converter"):
        write_json(ASSETS / "models/block" / f"{machine}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "down": f"metatech_reborn:block/{machine}_bottom",
                "up": f"metatech_reborn:block/{machine}_top",
                "north": f"metatech_reborn:block/{machine}_front",
                "south": f"metatech_reborn:block/{machine}_back",
                "west": f"metatech_reborn:block/{machine}_side",
                "east": f"metatech_reborn:block/{machine}_side",
                "particle": f"metatech_reborn:block/{machine}_side",
            },
        })
    print(f"Installed {len(NAMES)} approved 0.6.77 textures without GUI references")


if __name__ == "__main__":
    main()
