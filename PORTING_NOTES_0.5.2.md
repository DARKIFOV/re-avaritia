# MetaTech Reborn 0.5.2 work in progress

This branch restores MetaAdvanced/LoliEnergistics items and prepares direct AE2 9x9 pattern support for Forge 1.20.1.

## Restored item batch 1

- Meta Vajra with Forge Energy storage and 1x1 / 3x3 mode switching;
- Snow Gun with three firing modes;
- Skull Axe with creature-head drop logic;
- ten electric swords with stored energy, active mode and per-hit energy use.

## Restored item batch 2

### Rechargeable energy food

Four non-consumable rechargeable food tiers were ported from the old MetaAdvanced enum values:

| Tier | Hunger | Saturation modifier | Use duration | FE/use | Capacity | Receive limit |
|---:|---:|---:|---:|---:|---:|---:|
| I | 1 | 1.0 | 32 | 150 | 50,000 | 500 |
| II | 2 | 2.0 | 16 | 300 | 100,000 | 1,000 |
| III | 3 | 3.0 | 1 | 450 | 150,000 | 1,500 |
| IV | 20 | 20.0 | instant | 600 | 300,000 | 3,000 |

The item remains in the player's hand after use and spends Forge Energy instead of consuming an item stack.

### Wind rotors

Three configuration-defined rotor items were restored with their original durability and wind parameters:

| Rotor | Radius | Durability | Efficiency | Wind range |
|---|---:|---:|---:|---:|
| Iridium | 11 | 1,209,600 | 320 | 10–120 MCW |
| Quantum | 11 | 2,419,200 | 640 | 10–120 MCW |
| Ultimate | 11 | 4,838,400 | 1,280 | 10–120 MCW |

The items and durability are present. Their generator-machine integration will be connected when the corresponding wind generator is ported.

## Resources and validation

- item models and textures for both restored batches are included in the generated resource overlay;
- Russian and English translations are included;
- GitHub Actions run 124 completed successfully on Java 17 / Forge 1.20.1;
- the WIP artifact contains the main MetaTech Reborn JAR and the temporary AE2 9x9 companion JAR.

## Still pending

- crafting recipes for energy food and wind rotors;
- direct wind-generator compatibility for the restored rotors;
- direct in-mod port of the extreme molecular assembler and 9x9 pattern terminal;
- in-game testing and balancing.
