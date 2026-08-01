# MetaTech Reborn — Forge 1.20.1

Современный порт машин и предметов из старых `MetaAdvanced`, `MetaThaumcraft`, `Greenhouses`, LoliEnergistics и связанных аддонов для Minecraft 1.20.1 Forge.

## Текущая разработка

- рабочая ветка: `agent/0.5.5-unified-ae2`;
- черновой PR: [#6](https://github.com/DARKIFOV/re-avaritia/pull/6);
- текущая цель: один JAR MetaTech Reborn без отдельного `AE2 Molecular Assembler Plus`;
- GitHub Actions публикует один игровой JAR;
- версия остаётся WIP до проверки непосредственно в игре.

## Шаблоны AE2 9×9

Правильный предел — **36 шаблонов**:

- 9 слотов доступны без улучшения;
- улучшение ёмкости добавляет 27 слотов;
- итог: **9 + 27 = 36**.

Ранее упоминавшиеся значения 28 и 30 больше не используются.

Добавлены:

- пустой шаблон 9×9;
- закодированный шаблон 9×9;
- хранение 81 позиции рецепта и результата в NBT;
- внутренний банк молекулярного сборщика на 36 шаблонов;
- отдельное улучшение ёмкости;
- выбор шаблона по результату задания и ингредиентам, переданным AE2 Pattern Provider;
- отдельный терминал кодирования рецептов 9×9;
- кнопки кодирования и очистки сетки;
- поддержка рецептов Re-Avaritia и MetaTech.

Пока требуют проверки в игре:

- возврат результата в сеть AE2;
- выбор между похожими шаблонами;
- сохранение всех 36 слотов после перезапуска;
- перенос рецепта из JEI одним нажатием.

## Новый внешний вид мана-бура

Добавлен новый комплект текстур. Старый почти полностью фиолетовый вариант заменяется на более механический стиль:

- тёмная сталь;
- бронзовые рамы и крепления;
- бирюзовые линии маны;
- фиолетовый кристалл ядра;
- отдельные новые текстуры контроллера, корпуса, ядра и сопла;
- обновлённые иконки модуля и трёх улучшений;
- GUI мана-бура переведён в ту же палитру.

Изменённые файлы:

```text
textures/block/mana_drill_casing.png
textures/block/mana_drill_controller_front.png
textures/block/mana_drill_controller_side.png
textures/block/mana_drill_controller_top.png
textures/block/mana_drill_core.png
textures/block/mana_drill_nozzle.png
textures/gui/mana_drill.png
textures/item/mana_drill_module.png
textures/item/mana_drill_speed_upgrade.png
textures/item/mana_drill_looting_upgrade.png
textures/item/mana_drill_generation_upgrade.png
```

Полный перечень текстур: [`docs/TEXTURES-0.5.5-RU.md`](https://github.com/DARKIFOV/re-avaritia/blob/agent/0.5.5-unified-ae2/docs/TEXTURES-0.5.5-RU.md).

## Один JAR

Из ветки удалены:

- зависимость от `AE2 Molecular Assembler Plus`;
- копирование второго JAR в GitHub Actions;
- отдельный архив аддона.

Отдельно устанавливаются только обязательные базовые моды: Forge, AE2, Botania и Re-Avaritia.

## Молекулярный сборщик 9×9

- поддерживает рецепты Re-Avaritia `crafting_table_recipe`, включая shaped и shapeless;
- имеет сетку 9×9, выход, FE, прогресс и NBT-сохранение;
- реализует AE2 `ICraftingMachine`;
- принимает задания от Pattern Provider;
- использует нативный банк шаблонов MetaTech;
- реальные затраты FE исправлены.

## Объединитель нейтрония

- девять независимых входов;
- поддержка обычного, плотного, более плотного и максимально плотного сборщика Re-Avaritia;
- резервные рецепты при сбое датапака;
- приём FE кабелями и заряженными предметами;
- отдельные индикаторы каждого процесса;
- улучшения скорости, эффективности и выхода.

## Мана-бур

Многоблочная структура 3×3×3:

- контроллер расположен в центре внешней стенки;
- ядро находится за контроллером;
- сопло находится над ядром;
- блок над контроллером остаётся воздухом;
- остальные 23 позиции заполняются корпусами.

Поддерживается приём маны:

- лучами Mana Spreader;
- через Spark над контроллером;
- из ближайшего отдающего Mana Pool.

Улучшения:

- скорость I–V;
- добыча I–IX;
- генерация I–III.

## Теплица Botania

- слот цветка;
- 3 слота модулей;
- 6 слотов топлива;
- бак воды или лавы;
- внутренний буфер маны;
- передача маны в соседний Mana Pool;
- Endoflame, Hydroangeas, Gourmaryllis, Entropinnyum, Thermalily и Spectrolus;
- Economy, Efficiency, Speed, Infinite Day, Infinite Night и Infinite Lava.

## Перенесённые предметы

- мета-ваджра;
- снежная пушка;
- топор черепов;
- электрические мечи нескольких уровней;
- энергетическая еда I–IV;
- иридиевый, квантовый и предельный ветророторы.

## Зависимости

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- Re-Avaritia 1.4.1
- Applied Energistics 2 15.4.x
- Botania 1.20.1-454+
- JEI 15.x — необязательно

## Сборка

```bash
gradle build
```

Готовый JAR создаётся в `build/libs/`.

## Проверка

Инструкция: [`docs/TESTING-0.5.5-RU.md`](https://github.com/DARKIFOV/re-avaritia/blob/agent/0.5.5-unified-ae2/docs/TESTING-0.5.5-RU.md).

Изменения сливаются в `main` только после успешной GitHub Actions сборки и проверки непосредственно в игре.
