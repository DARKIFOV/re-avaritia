from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
GEN = ROOT / "build/generated/metatech_assets/assets/metatech_reborn"


def write_json(relative: str, data: object) -> None:
    target = GEN / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def mana_drill_controller_variants() -> dict:
    variants = {}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for formed in (False, True):
            for reversed_value in (False, True):
                key = f"facing={facing},formed={str(formed).lower()},reversed={str(reversed_value).lower()}"
                if formed:
                    variants[key] = {"model": "metatech_reborn:block/empty"}
                else:
                    entry = {"model": "metatech_reborn:block/mana_drill"}
                    if rotation:
                        entry["y"] = rotation
                    variants[key] = entry
    return {"variants": variants}


def mana_drill_nozzle_variants() -> dict:
    variants = {}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for formed in (False, True):
            key = f"facing={facing},formed={str(formed).lower()}"
            if formed:
                variants[key] = {"model": "metatech_reborn:block/empty"}
            else:
                entry = {"model": "metatech_reborn:block/mana_drill_nozzle"}
                if rotation:
                    entry["y"] = rotation
                variants[key] = entry
    return {"variants": variants}


def horizontal_variants(model: str) -> dict:
    return {
        "variants": {
            "facing=north": {"model": f"metatech_reborn:block/{model}"},
            "facing=east": {"model": f"metatech_reborn:block/{model}", "y": 90},
            "facing=south": {"model": f"metatech_reborn:block/{model}", "y": 180},
            "facing=west": {"model": f"metatech_reborn:block/{model}", "y": 270},
        }
    }


def main() -> None:
    # When the multiblock is formed, physical 3x3x3 parts are hidden and the
    # block-entity renderer draws the one-piece opaque machine. This avoids the
    # previous double-render/ghost shell while keeping every real block present.
    write_json("blockstates/mana_drill.json", mana_drill_controller_variants())
    write_json("blockstates/mana_drill_nozzle.json", mana_drill_nozzle_variants())
    write_json("blockstates/mana_drill_casing.json", {
        "variants": {
            "formed=false": {"model": "metatech_reborn:block/mana_drill_casing"},
            "formed=true": {"model": "metatech_reborn:block/empty"},
        }
    })
    write_json("blockstates/mana_drill_core.json", {
        "variants": {
            "formed=false": {"model": "metatech_reborn:block/mana_drill_core"},
            "formed=true": {"model": "metatech_reborn:block/empty"},
        }
    })

    # Dragon machines have real horizontal facing in code. Preserve the approved
    # PNGs and only wire the faces differently so the two machines are unmistakable.
    write_json("blockstates/extreme_dragon_assembler.json",
               horizontal_variants("extreme_dragon_assembler"))
    write_json("blockstates/dragon_pattern_encoder.json",
               horizontal_variants("dragon_pattern_encoder"))

    write_json("models/block/extreme_dragon_assembler.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "down": "metatech_reborn:block/extreme_dragon_assembler_bottom",
            "up": "metatech_reborn:block/extreme_dragon_assembler_top",
            "north": "metatech_reborn:block/extreme_dragon_assembler_front",
            # technical back instead of another dragon panel
            "south": "metatech_reborn:block/extreme_dragon_assembler_side",
            "west": "metatech_reborn:block/extreme_dragon_assembler_side",
            "east": "metatech_reborn:block/extreme_dragon_assembler_side",
            "particle": "metatech_reborn:block/extreme_dragon_assembler_side",
        }
    })
    write_json("models/block/dragon_pattern_encoder.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "down": "metatech_reborn:block/dragon_pattern_encoder_bottom",
            "up": "metatech_reborn:block/dragon_pattern_encoder_top",
            "north": "metatech_reborn:block/dragon_pattern_encoder_front",
            "south": "metatech_reborn:block/dragon_pattern_encoder_back",
            "west": "metatech_reborn:block/dragon_pattern_encoder_side",
            "east": "metatech_reborn:block/dragon_pattern_encoder_side",
            "particle": "metatech_reborn:block/dragon_pattern_encoder_side",
        }
    })

    # Inventory icons use the already-approved front PNG directly. This makes the
    # assembler (red energy capsule) and encoder (dragon panel) clearly different.
    write_json("models/item/extreme_dragon_assembler.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "metatech_reborn:block/extreme_dragon_assembler_front"}
    })
    write_json("models/item/dragon_pattern_encoder.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "metatech_reborn:block/dragon_pattern_encoder_front"}
    })

    print("Installed 0.6.107 resource-only visual fixes without generating or replacing PNGs")


if __name__ == "__main__":
    main()
