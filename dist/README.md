# MetaTech Reborn 0.6.77

Готовый JAR формируется GitHub Actions из ветки `main` и публикуется как artifact с именем:

```text
MetaTech-Reborn-unified-<commit>
```

Локально проверенный тестовый файл:

```text
metatech_reborn-0.6.77-final-gui-text-cleanup.jar
SHA-256: e358ed68779ad12e59952a541e2591e28a07fe4e77c51ec268e982cc3025c173
```

Контрольная сумма относится к локально собранному тестовому JAR. JAR, повторно созданный GitHub Actions, может иметь другую побайтовую сумму из-за повторной упаковки, но собирается из содержимого `main`.

Полноценный стабильный Release следует создавать после игрового регрессионного теста из `docs/ROADMAP.md`.
