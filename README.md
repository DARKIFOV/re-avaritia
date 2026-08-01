# MetaTech Reborn — Forge 1.20.1

Первый рабочий этап переноса механик `MetaAdvanced` с Minecraft 1.7.10 на Forge 1.20.1.

## Что реализовано

- блок **Молекулярный сборщик 9x9**;
- прямое чтение рецептов Re-Avaritia из `ModRecipeTypes.CRAFTING_TABLE_RECIPE`;
- поддержка shaped и shapeless рецептов через публичный `ITierCraftingRecipe` API;
- собственный datapack recipe type `metatech_reborn:molecular_assembling` как резервный вариант;
- сетка 9x9, выходной слот, FE-буфер и индикатор прогресса;
- сохранение инвентаря, энергии, прогресса и зафиксированного рецепта через NBT;
- защита от дюпа: результат вставляется только после повторной проверки рецепта и свободного выхода;
- Forge `IItemHandler` для труб, воронок и AE2 Pattern Provider;
- Forge Energy capability;
- автоматический вывод результата в соседний инвентарь;
- русский и английский языки;
- JEI-категория для собственных JSON-рецептов;
- optional-интеграция: отсутствие Re-Avaritia, AE2 или JEI не должно крашить сервер.

## Как работает AE2

1. Один раз вручную разложите правильный рецепт в сетке 9x9.
2. Сборщик найдёт рецепт Re-Avaritia и автоматически зафиксирует его.
3. После первого крафта сетка опустеет, но шаблон позиций сохранится.
4. Подключите AE2 Pattern Provider или другую автоматизацию к любой стороне блока.
5. Каждый входной слот принимает только предмет, записанный в его позиции шаблона, и имеет лимит 1 предмет. Поэтому одинаковые ингредиенты распределяются по нужным ячейкам, а не складываются в один слот.
6. Кнопка **«Снять фиксацию»** позволяет заменить рецепт.

Остаточные предметы рецепта, например пустые вёдра, остаются в соответствующих входных слотах и доступны через item capability.

## Требования

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17
- Re-Avaritia 1.4.1 — необязательно, но требуется для её рецептов
- Applied Energistics 2 15.x — необязательно; используется стандартная item capability
- JEI 15.20.0.112 — необязательно

## Сборка

Проект использует ForgeGradle 6.

```bash
gradle build
```

Готовый JAR появится в `build/libs/`.

При использовании IntelliJ IDEA откройте папку как Gradle-проект и выполните задачу:

```bash
gradle genIntellijRuns
```

Затем запускайте конфигурацию `runClient`.

## Re-Avaritia

`build.gradle` использует CurseMaven-файл Re-Avaritia из предоставленной сборки:

```gradle
compileOnly fg.deobf('curse.maven:re-avaritia-623969:8497793')
runtimeOnly fg.deobf('curse.maven:re-avaritia-623969:8497793')
```

Если CurseMaven недоступен, положите JAR Re-Avaritia в папку `libs/` и примените вариант из `libs/README.txt`.

## Собственные рецепты

Пример находится в:

`examples/datapack/data/metatech_reborn/recipes/example_9x9.json`

Формат:

```json
{
  "type": "metatech_reborn:molecular_assembling",
  "pattern": [
    "AAAAAAAAA",
    "AAAAAAAAA",
    "AAAAAAAAA",
    "AAAAAAAAA",
    "AAAABAAAA",
    "AAAAAAAAA",
    "AAAAAAAAA",
    "AAAAAAAAA",
    "AAAAAAAAA"
  ],
  "key": {
    "A": { "item": "minecraft:diamond" },
    "B": { "item": "minecraft:nether_star" }
  },
  "result": {
    "item": "minecraft:dragon_egg",
    "count": 1
  },
  "time": 1200,
  "energy_per_tick": 2000
}
```

## Конфиг

После первого запуска создаётся `config/metatech_reborn-common.toml`:

- включение интеграции Re-Avaritia;
- ёмкость и скорость приёма FE;
- время и FE/t для рецептов Re-Avaritia;
- автоматический вывод результата.

## Статус переноса

Эта версия — **MVP 1** из общего плана переноса. Она содержит реально реализованный 9x9-сборщик. Следующие очереди ещё не включены в этот архив:

- автоматическая экстремальная ковка;
- объединитель нейтрония;
- мана-бур Botania;
- теплицы Botania;
- сборщики 3x3, 5x5 и 7x7.

Старый код 1.7.10 не копировался: механика переписана под BlockEntity, Menu/Screen, Forge capabilities и современные recipe types.
