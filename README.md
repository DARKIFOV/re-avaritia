# MetaTech Reborn — Forge 1.20.1

Современный порт машин и предметов из старых `MetaAdvanced`, `MetaThaumcraft`, `Greenhouses`, LoliEnergistics и связанных аддонов для Minecraft 1.20.1 Forge.

## Текущая разработка

- рабочая ветка: `agent/0.5.6-greenhouse-textures`;
- PR: [#8](https://github.com/DARKIFOV/re-avaritia/pull/8);
- версия: `0.5.6-greenhouse-textures-WIP`;
- цель: один JAR MetaTech Reborn без отдельного `AE2 Molecular Assembler Plus`;
- изменения не считаются стабильными до проверки непосредственно в игре.

## Шаблоны AE2 9×9

Правильный предел — **36 шаблонов**:

- 9 слотов доступны без улучшения;
- улучшение ёмкости добавляет 27 слотов;
- итог: **9 + 27 = 36**.

Добавлены пустые и закодированные шаблоны 9×9, хранение 81 позиции рецепта, банк шаблонов, улучшение ёмкости и терминал кодирования.

## Текстуры мана-бура

В ветке уже используется выбранный комплект мана-бура:

- livingrock-серый корпус;
- livingwood/бронзовые крепления;
- бирюзовые мана-линии;
- зелёно-бирюзовое ядро;
- фиолетовые иконки модуля и улучшений.

## Текстуры теплицы — выбран вариант 4

В мод загружен **четвёртый вариант** промышленной магитех-теплицы.

Основной стиль:

- тёмный livingrock и усиленные металлические панели;
- бронзовые и деревянные крепления;
- яркие бирюзовые мана-линии;
- мох и растительность;
- отдельная стеклянная камера;
- отдельный круглый мана-порт.

Добавлены текстуры 32×32:

```text
textures/block/greenhouse_front.png
textures/block/greenhouse_side.png
textures/block/greenhouse_top.png
textures/block/greenhouse_bottom.png
textures/block/greenhouse_glass.png
textures/block/greenhouse_mana_port.png
textures/item/greenhouse_economy_module.png
textures/item/greenhouse_efficiency_module.png
textures/item/greenhouse_speed_module.png
textures/item/greenhouse_infinite_day_module.png
textures/item/greenhouse_infinite_night_module.png
textures/item/greenhouse_infinite_lava_module.png
```

Расположение граней:

- передняя грань — контроллер;
- задняя грань — стеклянная камера;
- правая грань — мана-порт;
- левая грань — техническая боковая панель;
- верх и низ имеют отдельные текстуры.

Теплица теперь имеет горизонтальное направление при установке, а blockstate вращает модель вслед за направлением игрока.

Модули Economy I–III, Efficiency I–III и Speed I–III используют собственные иконки соответствующего типа. Infinite Day, Infinite Night и Infinite Lava получили отдельные иконки солнца, луны и лавового контейнера.

## Молекулярный сборщик 9×9

- поддерживает рецепты Re-Avaritia `crafting_table_recipe`;
- имеет сетку 9×9, выход, FE, прогресс и NBT-сохранение;
- реализует AE2 `ICraftingMachine`;
- принимает задания от Pattern Provider;
- использует нативный банк шаблонов MetaTech.

## Объединитель нейтрония

- девять независимых входов;
- обычный, плотный, более плотный и максимально плотный сборщики Re-Avaritia;
- приём FE кабелями и заряженными предметами;
- улучшения скорости, эффективности и выхода.

## Мана-бур

Многоблочная структура 3×3×3. Поддерживается приём маны лучами Mana Spreader, через Spark и из ближайшего отдающего Mana Pool.

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

Изменения сливаются в `main` только после успешной GitHub Actions сборки и проверки непосредственно в игре.
