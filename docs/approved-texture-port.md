# Approved texture port

## Luck Converter port (0.6.75)

The approved Standard and Advanced Luck Converter reference sheets were ported into a test JAR.

### Included

- Standard Luck Converter: front, side, top, back and bottom textures.
- Advanced Luck Converter: front, side, top, back and bottom textures.
- 16x16 fallback textures generated from the new front faces.
- Standard and Advanced GUI reference PNGs packaged under `assets/metatech_reborn/textures/gui/`.
- Block models updated so south and down faces use the dedicated back and bottom textures.

### Explicitly unchanged

- `luck_module_*`
- `luck_speed_upgrade_*`
- `energy_food_*`

Fourteen existing module/upgrade/energy texture files were verified byte-for-byte unchanged.

### Runtime note

`LuckConverterScreen` is currently drawn in Java through `MetaTechGui`, so the approved GUI PNGs are packaged as reference resources but are not yet used as the live screen background. Wiring the approved GUI style to the real container slot layout remains a separate implementation step.

### Validation

- ZIP integrity passed.
- 110 PNG files validated.
- 200 JSON files parsed successfully.
- Test JAR SHA-256: `70ff9db6a04ee2db3a700376e986b833637162d8440da9f979183f1691d4ac86`.
