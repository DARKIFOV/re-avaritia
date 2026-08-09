from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> tuple[Path, str]:
    target = ROOT / path
    return target, target.read_text(encoding="utf-8")


def write(target: Path, text: str) -> None:
    target.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def patch_mod_items() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/registry/ModItems.java")

    marker = '''    public static final RegistryObject<Item> MANA_DRILL_MEKANISM_EXTRAS_MODULE = oreModule(
            "mana_drill_module_mekanism_extras");
'''
    replacement = marker + '''    public static final RegistryObject<Item> MANA_DRILL_MEKANISM_MODULE = oreModule(
            "mana_drill_module_mekanism");
    public static final RegistryObject<Item> MANA_DRILL_DRACONIC_EVOLUTION_MODULE = oreModule(
            "mana_drill_module_draconic_evolution");
'''
    text = replace_once(text, marker, replacement, "combined Mekanism and Draconic module registrations")

    old_list = '''        return List.of(MANA_DRILL_MODULE, MANA_DRILL_AD_ASTRA_MODULE, MANA_DRILL_THERMAL_MODULE,
                MANA_DRILL_EVOLVED_MEKANISM_MODULE, MANA_DRILL_MEKANISM_EXTRAS_MODULE,
                MANA_DRILL_POWAH_MODULE, MANA_DRILL_MYTHICBOTANY_MODULE,
                MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE, MANA_DRILL_OMNI_MODULE);
'''
    new_list = '''        return List.of(MANA_DRILL_MODULE, MANA_DRILL_AD_ASTRA_MODULE, MANA_DRILL_THERMAL_MODULE,
                MANA_DRILL_EVOLVED_MEKANISM_MODULE, MANA_DRILL_MEKANISM_EXTRAS_MODULE,
                MANA_DRILL_MEKANISM_MODULE, MANA_DRILL_DRACONIC_EVOLUTION_MODULE,
                MANA_DRILL_POWAH_MODULE, MANA_DRILL_MYTHICBOTANY_MODULE,
                MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE, MANA_DRILL_OMNI_MODULE);
'''
    text = replace_once(text, old_list, new_list, "mana drill module compatibility list")
    write(target, text)


def patch_creative_tab() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java")

    old = '''            if (ModList.get().isLoaded("evolvedmekanism")) event.accept(ModItems.MANA_DRILL_EVOLVED_MEKANISM_MODULE.get());
            if (ModList.get().isLoaded("mekanism_extras")) event.accept(ModItems.MANA_DRILL_MEKANISM_EXTRAS_MODULE.get());
            if (ModList.get().isLoaded("powah")) event.accept(ModItems.MANA_DRILL_POWAH_MODULE.get());
            if (ModList.get().isLoaded("mythicbotany")) event.accept(ModItems.MANA_DRILL_MYTHICBOTANY_MODULE.get());
            if (ModList.get().isLoaded("mysticalagriculture")) event.accept(ModItems.MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE.get());
            if (ModList.get().isLoaded("ad_astra")
                    && ModList.get().isLoaded("thermal_foundation")
                    && ModList.get().isLoaded("evolvedmekanism")
                    && ModList.get().isLoaded("mekanism_extras")
                    && ModList.get().isLoaded("powah")
                    && ModList.get().isLoaded("mythicbotany")
                    && ModList.get().isLoaded("mysticalagriculture")) {
                event.accept(ModItems.MANA_DRILL_OMNI_MODULE.get());
            }
'''
    new = '''            if (ModList.get().isLoaded("evolvedmekanism") && ModList.get().isLoaded("mekanism_extras")) {
                event.accept(ModItems.MANA_DRILL_MEKANISM_MODULE.get());
            }
            if (ModList.get().isLoaded("draconicevolution")) {
                event.accept(ModItems.MANA_DRILL_DRACONIC_EVOLUTION_MODULE.get());
            }
            if (ModList.get().isLoaded("powah")) event.accept(ModItems.MANA_DRILL_POWAH_MODULE.get());
            if (ModList.get().isLoaded("mythicbotany")) event.accept(ModItems.MANA_DRILL_MYTHICBOTANY_MODULE.get());
            if (ModList.get().isLoaded("mysticalagriculture")) event.accept(ModItems.MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE.get());
            if (ModList.get().isLoaded("ad_astra")
                    && ModList.get().isLoaded("thermal_foundation")
                    && ModList.get().isLoaded("evolvedmekanism")
                    && ModList.get().isLoaded("mekanism_extras")
                    && ModList.get().isLoaded("powah")
                    && ModList.get().isLoaded("mythicbotany")
                    && ModList.get().isLoaded("mysticalagriculture")
                    && ModList.get().isLoaded("draconicevolution")) {
                event.accept(ModItems.MANA_DRILL_OMNI_MODULE.get());
            }
'''
    text = replace_once(text, old, new, "creative tab Mekanism merge and Draconic module")
    write(target, text)


if __name__ == "__main__":
    patch_mod_items()
    patch_creative_tab()
    print("Applied 0.6.92 combined Mekanism + Draconic Evolution mana-drill module patches")
