from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_LANG = ROOT / "src/main/resources/assets/metatech_reborn/lang"
GENERATED_LANG = ROOT / "build/generated/metatech_assets/assets/metatech_reborn/lang"

CLEANUP = {
    "ru_ru": {
        # Молекулярный сборщик: энергия, прогресс и короткий статус.
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
        "gui.metatech_reborn.assembler.status.ae2_ready": "Готов",
        "gui.metatech_reborn.assembler.status.energy": "Нет FE",
        "gui.metatech_reborn.assembler.status.idle": "",
        "gui.metatech_reborn.assembler.status.output": "Выход полон",
        "gui.metatech_reborn.assembler.status.recipe": "Нет рецепта",
        "gui.metatech_reborn.assembler.status.running": "Работа",
        "gui.metatech_reborn.assembler.tooltip.capacity": "",
        "gui.metatech_reborn.assembler.tooltip.pattern_bank": "",
        "gui.metatech_reborn.assembler.tooltip.speed_cards": "",

        # Кодировщик: кнопки и короткий статус.
        "gui.metatech_reborn.blank_pattern": "",
        "gui.metatech_reborn.encoded_pattern": "",
        "gui.metatech_reborn.encoder.jei_hint": "",
        "gui.metatech_reborn.encoder.encoded": "Закодирован",
        "gui.metatech_reborn.encoder.no_blank": "Нет шаблона",
        "gui.metatech_reborn.encoder.no_recipe": "Нет рецепта",
        "gui.metatech_reborn.encoder.output_blocked": "Заберите шаблон",
        "gui.metatech_reborn.encoder.ready": "Готов",

        # Преобразователи удачи: без постоянных подписей и статистической строки.
        "gui.metatech_reborn.luck_converter.inputs": "",
        "gui.metatech_reborn.luck_converter.outputs": "",
        "gui.metatech_reborn.luck_converter.upgrades": "",
        "gui.metatech_reborn.luck_converter.module": "",
        "gui.metatech_reborn.luck_converter.energy_slot": "",
        "gui.metatech_reborn.luck_converter.speed_active": "",
        "gui.metatech_reborn.luck_converter.stats": "",
        "gui.metatech_reborn.luck_converter.speed.none": "",
        "gui.metatech_reborn.luck_converter.speed.instant": "Мгновенно",
        "gui.metatech_reborn.luck_converter.speed.percent": "+%s%%",
        "gui.metatech_reborn.luck_converter.status.idle": "",
        "gui.metatech_reborn.luck_converter.status.no_energy": "Нет FE",
        "gui.metatech_reborn.luck_converter.status.no_input": "Нет входа",
        "gui.metatech_reborn.luck_converter.status.no_module": "Нужен модуль",
        "gui.metatech_reborn.luck_converter.status.output_full": "Выход полон",
        "gui.metatech_reborn.luck_converter.status.running": "Работа",
        "gui.metatech_reborn.luck_converter.tooltip.module": "",
        "gui.metatech_reborn.luck_converter.tooltip.speed": "",
        "gui.metatech_reborn.luck_converter.tooltip.upgrades": "",

        # Мана-бур.
        "gui.metatech_reborn.mana_drill.mana": "Мана: %1$s / %2$s",

        # Объединитель нейтрония: прогресс и короткий статус.
        "gui.metatech_reborn.neutron.collectors": "",
        "gui.metatech_reborn.neutron.outputs": "",
        "gui.metatech_reborn.neutron.stack_hint": "",
        "gui.metatech_reborn.neutron.upgrades": "",
        "gui.metatech_reborn.neutron.energy": "",
        "gui.metatech_reborn.neutron_upgrades": "",
        "gui.metatech_reborn.neutron.status.energy": "Нет FE",
        "gui.metatech_reborn.neutron.status.idle": "",
        "gui.metatech_reborn.neutron.status.output": "Выход полон",
        "gui.metatech_reborn.neutron.status.recipe": "Нет рецепта",
        "gui.metatech_reborn.neutron.status.running": "Работа",
        "gui.metatech_reborn.neutron.tooltip.process": "",
        "gui.metatech_reborn.neutron.tooltip.upgrades": "",

        # Теплица: только широкие индикаторы маны и жидкости.
        "gui.metatech_reborn.greenhouse.flower": "",
        "gui.metatech_reborn.greenhouse.fuel": "",
        "gui.metatech_reborn.greenhouse.modules": "",
        "gui.metatech_reborn.greenhouse.flower_stack": "",
        "gui.metatech_reborn.greenhouse.levels": "",
        "gui.metatech_reborn.greenhouse.levels_short": "",
        "gui.metatech_reborn.greenhouse.fluid_short": "Жидк.: %1$s / %2$s мБ",
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
        "gui.metatech_reborn.assembler.status.ae2_ready": "Ready",
        "gui.metatech_reborn.assembler.status.energy": "No FE",
        "gui.metatech_reborn.assembler.status.idle": "",
        "gui.metatech_reborn.assembler.status.output": "Output full",
        "gui.metatech_reborn.assembler.status.recipe": "No recipe",
        "gui.metatech_reborn.assembler.status.running": "Working",
        "gui.metatech_reborn.assembler.tooltip.capacity": "",
        "gui.metatech_reborn.assembler.tooltip.pattern_bank": "",
        "gui.metatech_reborn.assembler.tooltip.speed_cards": "",

        "gui.metatech_reborn.blank_pattern": "",
        "gui.metatech_reborn.encoded_pattern": "",
        "gui.metatech_reborn.encoder.jei_hint": "",
        "gui.metatech_reborn.encoder.encoded": "Encoded",
        "gui.metatech_reborn.encoder.no_blank": "No pattern",
        "gui.metatech_reborn.encoder.no_recipe": "No recipe",
        "gui.metatech_reborn.encoder.output_blocked": "Take pattern",
        "gui.metatech_reborn.encoder.ready": "Ready",

        "gui.metatech_reborn.luck_converter.inputs": "",
        "gui.metatech_reborn.luck_converter.outputs": "",
        "gui.metatech_reborn.luck_converter.upgrades": "",
        "gui.metatech_reborn.luck_converter.module": "",
        "gui.metatech_reborn.luck_converter.energy_slot": "",
        "gui.metatech_reborn.luck_converter.speed_active": "",
        "gui.metatech_reborn.luck_converter.stats": "",
        "gui.metatech_reborn.luck_converter.speed.none": "",
        "gui.metatech_reborn.luck_converter.speed.instant": "Instant",
        "gui.metatech_reborn.luck_converter.speed.percent": "+%s%%",
        "gui.metatech_reborn.luck_converter.status.idle": "",
        "gui.metatech_reborn.luck_converter.status.no_energy": "No FE",
        "gui.metatech_reborn.luck_converter.status.no_input": "No input",
        "gui.metatech_reborn.luck_converter.status.no_module": "Need module",
        "gui.metatech_reborn.luck_converter.status.output_full": "Output full",
        "gui.metatech_reborn.luck_converter.status.running": "Working",
        "gui.metatech_reborn.luck_converter.tooltip.module": "",
        "gui.metatech_reborn.luck_converter.tooltip.speed": "",
        "gui.metatech_reborn.luck_converter.tooltip.upgrades": "",

        "gui.metatech_reborn.mana_drill.mana": "Mana: %1$s / %2$s",

        "gui.metatech_reborn.neutron.collectors": "",
        "gui.metatech_reborn.neutron.outputs": "",
        "gui.metatech_reborn.neutron.stack_hint": "",
        "gui.metatech_reborn.neutron.upgrades": "",
        "gui.metatech_reborn.neutron.energy": "",
        "gui.metatech_reborn.neutron_upgrades": "",
        "gui.metatech_reborn.neutron.status.energy": "No FE",
        "gui.metatech_reborn.neutron.status.idle": "",
        "gui.metatech_reborn.neutron.status.output": "Output full",
        "gui.metatech_reborn.neutron.status.recipe": "No recipe",
        "gui.metatech_reborn.neutron.status.running": "Working",
        "gui.metatech_reborn.neutron.tooltip.process": "",
        "gui.metatech_reborn.neutron.tooltip.upgrades": "",

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
