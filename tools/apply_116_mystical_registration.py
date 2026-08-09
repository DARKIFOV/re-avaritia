from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str, label: str):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise RuntimeError(f"{label}: anchor not found")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

# Main registries + optional AE2 provider.
path = "src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java"
patch(path,
      "import ru.rfvv.metatechreborn.integration.ae2.MolecularAssemblerAe2Provider;\n",
      "import ru.rfvv.metatechreborn.integration.ae2.MolecularAssemblerAe2Provider;\n"
      "import ru.rfvv.metatechreborn.integration.ae2.MysticalInfusionAssemblerAe2Provider;\n",
      "mystical AE2 import")
patch(path,
      "import ru.rfvv.metatechreborn.registry.ModMenus;\n",
      "import ru.rfvv.metatechreborn.registry.ModMenus;\n"
      "import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;\n",
      "mystical registry import")
patch(path,
      "        ModRecipes.register(modBus);\n",
      "        ModRecipes.register(modBus);\n        ModMysticalInfusion.register(modBus);\n",
      "mystical registry call")
patch(path,
      "            MolecularAssemblerAe2Provider.register();\n",
      "            MolecularAssemblerAe2Provider.register();\n"
      "            MysticalInfusionAssemblerAe2Provider.register();\n",
      "mystical AE2 provider registration")

# Client screens. This runs after Dragon screen hotfix generation.
path = "src/main/java/ru/rfvv/metatechreborn/client/ClientModEvents.java"
patch(path,
      "import ru.rfvv.metatechreborn.client.screen.MolecularAssemblerScreen;\n",
      "import ru.rfvv.metatechreborn.client.screen.MolecularAssemblerScreen;\n"
      "import ru.rfvv.metatechreborn.client.screen.MysticalInfusionAssemblerScreen;\n"
      "import ru.rfvv.metatechreborn.client.screen.MysticalInfusionEncoderScreen;\n",
      "mystical screen imports")
patch(path,
      "import ru.rfvv.metatechreborn.registry.ModMenus;\n",
      "import ru.rfvv.metatechreborn.registry.ModMenus;\n"
      "import ru.rfvv.metatechreborn.registry.ModMysticalInfusion;\n",
      "mystical client registry import")
patch(path,
      "            MenuScreens.register(ModMenus.EXTREME_PATTERN_ENCODER.get(), ExtremePatternEncoderScreen::new);\n",
      "            MenuScreens.register(ModMenus.EXTREME_PATTERN_ENCODER.get(), ExtremePatternEncoderScreen::new);\n"
      "            MenuScreens.register(ModMysticalInfusion.ASSEMBLER_MENU.get(), MysticalInfusionAssemblerScreen::new);\n"
      "            MenuScreens.register(ModMysticalInfusion.ENCODER_MENU.get(), MysticalInfusionEncoderScreen::new);\n",
      "mystical client screen registration")

# Missing import guard in the encoder block generated for 0.6.116.
path = "src/main/java/ru/rfvv/metatechreborn/block/MysticalInfusionEncoderBlock.java"
p = ROOT / path
text = p.read_text(encoding="utf-8")
if "import net.minecraft.world.level.block.Block;" not in text:
    text = text.replace("import net.minecraft.world.level.block.BaseEntityBlock;\n",
                        "import net.minecraft.world.level.block.BaseEntityBlock;\nimport net.minecraft.world.level.block.Block;\n")
p.write_text(text, encoding="utf-8")

print("Registered Mystical Agriculture infusion machines")
