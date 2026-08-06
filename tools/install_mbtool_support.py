from __future__ import annotations

import gzip
import io
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATED = ROOT / "build/generated/metatech_assets"
ASSETS = GENERATED / "assets/metatech_reborn"
DATA = GENERATED / "data/metatech_reborn"

TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


def i8(value: int) -> bytes:
    return struct.pack(">b", value)


def i16(value: int) -> bytes:
    return struct.pack(">h", value)


def i32(value: int) -> bytes:
    return struct.pack(">i", value)


def i64(value: int) -> bytes:
    return struct.pack(">q", value)


def string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def tag_payload(tag_type: int, value: object) -> bytes:
    if tag_type == TAG_BYTE:
        return i8(int(value))
    if tag_type == TAG_SHORT:
        return i16(int(value))
    if tag_type == TAG_INT:
        return i32(int(value))
    if tag_type == TAG_LONG:
        return i64(int(value))
    if tag_type == TAG_STRING:
        return string(str(value))
    if tag_type == TAG_LIST:
        element_type, values = value
        return i8(element_type) + i32(len(values)) + b"".join(
            tag_payload(element_type, entry) for entry in values
        )
    if tag_type == TAG_COMPOUND:
        result = bytearray()
        for name, (child_type, child_value) in value.items():
            result += i8(child_type)
            result += string(name)
            result += tag_payload(child_type, child_value)
        result += b"\x00"
        return bytes(result)
    if tag_type == TAG_BYTE_ARRAY:
        raw = bytes(value)
        return i32(len(raw)) + raw
    if tag_type == TAG_INT_ARRAY:
        return i32(len(value)) + b"".join(i32(entry) for entry in value)
    if tag_type == TAG_LONG_ARRAY:
        return i32(len(value)) + b"".join(i64(entry) for entry in value)
    raise ValueError(f"Unsupported NBT tag type: {tag_type}")


def compressed_root(compound: dict[str, tuple[int, object]]) -> bytes:
    raw = i8(TAG_COMPOUND) + string("") + tag_payload(TAG_COMPOUND, compound)
    output = io.BytesIO()
    with gzip.GzipFile(fileobj=output, mode="wb", mtime=0) as archive:
        archive.write(raw)
    return output.getvalue()


def state(name: str, properties: dict[str, str] | None = None) -> dict[str, tuple[int, object]]:
    entry: dict[str, tuple[int, object]] = {"Name": (TAG_STRING, name)}
    if properties:
        entry["Properties"] = (
            TAG_COMPOUND,
            {key: (TAG_STRING, value) for key, value in properties.items()},
        )
    return entry


def make_mana_drill_structure() -> bytes:
    # Reference orientation: controller faces north and the 3-block depth extends south.
    # MultiBuilder Tool rotates horizontal DirectionProperty values automatically.
    palette = [
        state("minecraft:air"),
        state("metatech_reborn:mana_drill_casing", {"formed": "false"}),
        state(
            "metatech_reborn:mana_drill",
            {"facing": "north", "formed": "false", "reversed": "false"},
        ),
        state("metatech_reborn:mana_drill_core", {"formed": "false"}),
        state(
            "metatech_reborn:mana_drill_nozzle",
            {"facing": "north", "formed": "false"},
        ),
    ]

    blocks: list[dict[str, tuple[int, object]]] = []
    for y in range(3):
        for z in range(3):
            for x in range(3):
                position = (x, y, z)
                if position == (1, 1, 0):
                    palette_index = 2
                elif position == (1, 1, 1):
                    palette_index = 3
                elif position == (1, 2, 1):
                    palette_index = 4
                elif position == (1, 2, 0):
                    palette_index = 0
                else:
                    palette_index = 1
                blocks.append(
                    {
                        "pos": (TAG_LIST, (TAG_INT, [x, y, z])),
                        "state": (TAG_INT, palette_index),
                    }
                )

    root = {
        "size": (TAG_LIST, (TAG_INT, [3, 3, 3])),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, blocks)),
        "palette": (TAG_LIST, (TAG_COMPOUND, palette)),
        "DataVersion": (TAG_INT, 3465),
    }
    return compressed_root(root)


def patch_lang(locale: str, entries: dict[str, str]) -> None:
    source = ROOT / "src/main/resources/assets/metatech_reborn/lang" / f"{locale}.json"
    generated = ASSETS / "lang" / f"{locale}.json"
    data: dict[str, str] = {}
    if source.exists():
        data.update(json.loads(source.read_text(encoding="utf-8")))
    if generated.exists():
        data.update(json.loads(generated.read_text(encoding="utf-8")))
    data.update(entries)
    generated.parent.mkdir(parents=True, exist_ok=True)
    generated.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    target = DATA / "mbtool_structures/mana_drill.nbt"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(make_mana_drill_structure())

    patch_lang(
        "ru_ru",
        {"mbtool.structure.mana_drill": "Мана-бур MetaTech Reborn"},
    )
    patch_lang(
        "en_us",
        {"mbtool.structure.mana_drill": "MetaTech Reborn Mana Drill"},
    )
    print("Installed MultiBuilder Tool structure for the 3x3x3 Mana Drill")


if __name__ == "__main__":
    main()
