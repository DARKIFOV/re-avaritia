from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATED = ROOT / "build/generated/metatech_assets/assets/metatech_reborn"


def write_json(relative: str, data: dict) -> None:
    target = GENERATED / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def horizontal_variants(model: str) -> dict:
    return {
        "variants": {
            "facing=north": {"model": model},
            "facing=east": {"model": model, "y": 90},
            "facing=south": {"model": model, "y": 180},
            "facing=west": {"model": model, "y": 270},
        }
    }


def cube_model(prefix: str, has_back: bool = False) -> dict:
    side = f"metatech_reborn:block/{prefix}_side"
    textures = {
        "down": f"metatech_reborn:block/{prefix}_bottom" if has_back else side,
        "up": f"metatech_reborn:block/{prefix}_top",
        "north": f"metatech_reborn:block/{prefix}_front",
        "south": f"metatech_reborn:block/{prefix}_back" if has_back else side,
        "west": side,
        "east": side,
        "particle": side,
    }
    return {"parent": "minecraft:block/cube", "textures": textures}


def main() -> None:
    blocks = {
        "extreme_pattern_encoder": (False, "metatech_reborn:block/extreme_pattern_encoder"),
        "neutronium_combiner": (False, "metatech_reborn:block/neutronium_combiner"),
        "luck_converter": (True, "metatech_reborn:block/luck_converter"),
        "advanced_luck_converter": (True, "metatech_reborn:block/advanced_luck_converter"),
    }

    for name, (_, model) in blocks.items():
        write_json(f"blockstates/{name}.json", horizontal_variants(model))

    write_json("models/block/extreme_pattern_encoder.json",
               cube_model("extreme_pattern_encoder"))
    write_json("models/block/neutronium_combiner.json",
               cube_model("neutronium_combiner"))
    write_json("models/block/luck_converter.json",
               cube_model("luck_converter", has_back=True))
    write_json("models/block/advanced_luck_converter.json",
               cube_model("advanced_luck_converter", has_back=True))

    print("Installed 0.6.82 directional machine blockstates and approved texture models")


if __name__ == "__main__":
    main()
