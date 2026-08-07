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

    marker = '''    public static final RegistryObject<Item> MANA_DRILL_MODULE = ITEMS.register(
            "mana_drill_module", () -> new Item(new Item.Properties().stacksTo(1)));
'''
    additions = marker + '''    public static final RegistryObject<Item> MANA_DRILL_AD_ASTRA_MODULE = oreModule(
            "mana_drill_module_ad_astra");
    public static final RegistryObject<Item> MANA_DRILL_THERMAL_MODULE = oreModule(
            "mana_drill_module_thermal");
    public static final RegistryObject<Item> MANA_DRILL_EVOLVED_MEKANISM_MODULE = oreModule(
            "mana_drill_module_evolved_mekanism");
    public static final RegistryObject<Item> MANA_DRILL_MEKANISM_EXTRAS_MODULE = oreModule(
            "mana_drill_module_mekanism_extras");
    public static final RegistryObject<Item> MANA_DRILL_POWAH_MODULE = oreModule(
            "mana_drill_module_powah");
    public static final RegistryObject<Item> MANA_DRILL_MYTHICBOTANY_MODULE = oreModule(
            "mana_drill_module_mythicbotany");
    public static final RegistryObject<Item> MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE = oreModule(
            "mana_drill_module_mystical_agriculture");
'''
    text = replace_once(text, marker, additions, "mana drill ore module registrations")

    helper_marker = '''    private static RegistryObject<Item> upgrade(String name, ManaDrillUpgradeItem.Type type, int level) {
        return ITEMS.register(name, () -> new ManaDrillUpgradeItem(type, level));
    }
'''
    helper = '''    private static RegistryObject<Item> oreModule(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
    }

''' + helper_marker
    text = replace_once(text, helper_marker, helper, "ore module helper")

    list_marker = '''    public static List<RegistryObject<Item>> manaDrillUpgradeItems() {
'''
    module_list = '''    public static List<RegistryObject<Item>> manaDrillModuleItems() {
        return List.of(MANA_DRILL_MODULE, MANA_DRILL_AD_ASTRA_MODULE, MANA_DRILL_THERMAL_MODULE,
                MANA_DRILL_EVOLVED_MEKANISM_MODULE, MANA_DRILL_MEKANISM_EXTRAS_MODULE,
                MANA_DRILL_POWAH_MODULE, MANA_DRILL_MYTHICBOTANY_MODULE,
                MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE);
    }

''' + list_marker
    text = replace_once(text, list_marker, module_list, "mana drill module list")
    write(target, text)


def patch_mana_drill() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/blockentity/ManaDrillBlockEntity.java")
    old_valid = '''                case MODULE_SLOT -> stack.is(ModItems.MANA_DRILL_MODULE.get());
'''
    new_valid = '''                case MODULE_SLOT -> ModItems.manaDrillModuleItems().stream()
                        .anyMatch(module -> stack.is(module.get()));
'''
    text = replace_once(text, old_valid, new_valid, "mana drill module slot validation")

    old_quartz_anchor = '''                        new ManaDrillRecipe.Drop(new ItemStack(Items.LAPIS_LAZULI), 2, 6, 2_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.DIAMOND), 1, 2, 650),
'''
    new_quartz_anchor = '''                        new ManaDrillRecipe.Drop(new ItemStack(Items.LAPIS_LAZULI), 2, 6, 2_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.QUARTZ), 2, 8, 4_500),
                        new ManaDrillRecipe.Drop(new ItemStack(Items.DIAMOND), 1, 2, 650),
'''
    text = replace_once(text, old_quartz_anchor, new_quartz_anchor, "fallback quartz drop")
    write(target, text)


def patch_creative_tab() -> None:
    target, text = read("src/main/java/ru/rfvv/metatechreborn/MetaTechReborn.java")
    marker = '''            event.accept(ModItems.MANA_DRILL_MODULE.get());
            ModItems.manaDrillUpgradeItems().forEach(item -> event.accept(item.get()));
'''
    replacement = '''            event.accept(ModItems.MANA_DRILL_MODULE.get());
            if (ModList.get().isLoaded("ad_astra")) event.accept(ModItems.MANA_DRILL_AD_ASTRA_MODULE.get());
            if (ModList.get().isLoaded("thermal_foundation")) event.accept(ModItems.MANA_DRILL_THERMAL_MODULE.get());
            if (ModList.get().isLoaded("evolvedmekanism")) event.accept(ModItems.MANA_DRILL_EVOLVED_MEKANISM_MODULE.get());
            if (ModList.get().isLoaded("mekanism_extras")) event.accept(ModItems.MANA_DRILL_MEKANISM_EXTRAS_MODULE.get());
            if (ModList.get().isLoaded("powah")) event.accept(ModItems.MANA_DRILL_POWAH_MODULE.get());
            if (ModList.get().isLoaded("mythicbotany")) event.accept(ModItems.MANA_DRILL_MYTHICBOTANY_MODULE.get());
            if (ModList.get().isLoaded("mysticalagriculture")) event.accept(ModItems.MANA_DRILL_MYSTICAL_AGRICULTURE_MODULE.get());
            ModItems.manaDrillUpgradeItems().forEach(item -> event.accept(item.get()));
'''
    text = replace_once(text, marker, replacement, "conditional mana drill modules in creative tab")
    write(target, text)


if __name__ == "__main__":
    patch_mod_items()
    patch_mana_drill()
    patch_creative_tab()
    print("Applied 0.6.89 mana drill ore-module source patches")
