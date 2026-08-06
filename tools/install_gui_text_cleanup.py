from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_LANG = ROOT / "src/main/resources/assets/metatech_reborn/lang"
GENERATED_LANG = ROOT / "build/generated/metatech_assets/assets/metatech_reborn/lang"

CLEANUP = {
    "ru_ru": {
        "gui.metatech_reborn.assembler.output": "",
        "gui.metatech_reborn.assembler.charge": "",
        "gui.metatech_reborn.recipe_locked": "",
        "gui.metatech_reborn.recipe_unlocked": "",
        "gui.metatech_reborn.assembler.speed_cards": "",
        "gui.metatech_reborn.pattern_bank": "",
        "gui.metatech_reborn.pattern_count": "",
        "gui.metatech_reborn.pattern_capacity_slot": "",
        "gui.metatech_reborn.ae2_speed_cards": "",
        "gui.metatech_reborn.ae2_native_patterns": "",
        "gui.metatech_reborn.blank_pattern": "",
        "gui.metatech_reborn.encoded_pattern": "",
        "gui.metatech_reborn.encoder.jei_hint": "",
        "gui.metatech_reborn.encoder.encoded": "Закодирован",
        "gui.metatech_reborn.encoder.no_blank": "Нет шаблона",
        "gui.metatech_reborn.encoder.ready": "Готов",
        "gui.metatech_reborn.luck_converter.inputs": "",
        "gui.metatech_reborn.luck_converter.outputs": "",
        "gui.metatech_reborn.luck_converter.upgrades": "",
        "gui.metatech_reborn.luck_converter.module": "",
        "gui.metatech_reborn.luck_converter.energy_slot": "",
        "gui.metatech_reborn.luck_converter.speed_active": "",
        "gui.metatech_reborn.luck_converter.stats": "",
        "gui.metatech_reborn.luck_converter.speed.none": "",
        "gui.metatech_reborn.neutron.collectors": "",
        "gui.metatech_reborn.neutron.outputs": "",
        "gui.metatech_reborn.neutron.stack_hint": "",
        "gui.metatech_reborn.neutron.upgrades": "",
        "gui.metatech_reborn.neutron.energy": "",
        "gui.metatech_reborn.neutron_upgrades": "",
        "gui.metatech_reborn.greenhouse.flower": "",
        "gui.metatech_reborn.greenhouse.fuel": "",
        "gui.metatech_reborn.greenhouse.modules": "",
        "gui.metatech_reborn.greenhouse.flower_stack": "",
        "gui.metatech_reborn.greenhouse.levels": "",
        "gui.metatech_reborn.greenhouse.levels_short": "",
    },
    "en_us": {
        "gui.metatech_reborn.assembler.output": "",
        "gui.metatech_reborn.assembler.charge": "",
        "gui.metatech_reborn.recipe_locked": "",
        "gui.metatech_reborn.recipe_unlocked": "",
        "gui.metatech_reborn.assembler.speed_cards": "",
        "gui.metatech_reborn.pattern_bank": "",
        "gui.metatech_reborn.pattern_count": "",
        "gui.metatech_reborn.pattern_capacity_slot": "",
        "gui.metatech_reborn.ae2_speed_cards": "",
        "gui.metatech_reborn.ae2_native_patterns": "",
        "gui.metatech_reborn.blank_pattern": "",
        "gui.metatech_reborn.encoded_pattern": "",
        "gui.metatech_reborn.encoder.jei_hint": "",
        "gui.metatech_reborn.encoder.encoded": "Encoded",
        "gui.metatech_reborn.encoder.no_blank": "No pattern",
        "gui.metatech_reborn.encoder.ready": "Ready",
        "gui.metatech_reborn.luck_converter.inputs": "",
        "gui.metatech_reborn.luck_converter.outputs": "",
        "gui.metatech_reborn.luck_converter.upgrades": "",
        "gui.metatech_reborn.luck_converter.module": "",
        "gui.metatech_reborn.luck_converter.energy_slot": "",
        "gui.metatech_reborn.luck_converter.speed_active": "",
        "gui.metatech_reborn.luck_converter.stats": "",
        "gui.metatech_reborn.luck_converter.speed.none": "",
        "gui.metatech_reborn.neutron.collectors": "",
        "gui.metatech_reborn.neutron.outputs": "",
        "gui.metatech_reborn.neutron.stack_hint": "",
        "gui.metatech_reborn.neutron.upgrades": "",
        "gui.metatech_reborn.neutron.energy": "",
        "gui.metatech_reborn.neutron_upgrades": "",
        "gui.metatech_reborn.greenhouse.flower": "",
        "gui.metatech_reborn.greenhouse.fuel": "",
        "gui.metatech_reborn.greenhouse.modules": "",
        "gui.metatech_reborn.greenhouse.flower_stack": "",
        "gui.metatech_reborn.greenhouse.levels": "",
        "gui.metatech_reborn.greenhouse.levels_short": "",
    },
}


def patch(path: Path, values: dict[str, str]) -> None:
    data: dict[str, str] = {}
    if path.exists():
        data.update(json.loads(path.read_text(encoding="utf-8")))
    data.update(values)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


for locale, values in CLEANUP.items():
    patch(SOURCE_LANG / f"{locale}.json", values)
    patch(GENERATED_LANG / f"{locale}.json", values)

print("Installed final visible GUI text cleanup")
