package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.item.BlankExtremePatternItem;
import ru.rfvv.metatechreborn.item.EncodedExtremePatternItem;
import ru.rfvv.metatechreborn.item.GreenhouseModuleItem;
import ru.rfvv.metatechreborn.item.ManaDrillUpgradeItem;
import ru.rfvv.metatechreborn.item.PatternCapacityUpgradeItem;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Item> MOLECULAR_ASSEMBLER_9X9 = blockItem(
            "molecular_assembler_9x9", ModBlocks.MOLECULAR_ASSEMBLER_9X9);
    public static final RegistryObject<Item> MANA_DRILL = blockItem(
            "mana_drill", ModBlocks.MANA_DRILL);
    public static final RegistryObject<Item> MANA_DRILL_CASING = blockItem(
            "mana_drill_casing", ModBlocks.MANA_DRILL_CASING);
    public static final RegistryObject<Item> MANA_DRILL_CORE = blockItem(
            "mana_drill_core", ModBlocks.MANA_DRILL_CORE);
    public static final RegistryObject<Item> MANA_DRILL_NOZZLE = blockItem(
            "mana_drill_nozzle", ModBlocks.MANA_DRILL_NOZZLE);
    public static final RegistryObject<Item> GREENHOUSE = blockItem(
            "greenhouse", ModBlocks.GREENHOUSE);

    public static final RegistryObject<Item> BLANK_EXTREME_PATTERN = ITEMS.register(
            "blank_extreme_pattern", BlankExtremePatternItem::new);
    public static final RegistryObject<Item> ENCODED_EXTREME_PATTERN = ITEMS.register(
            "encoded_extreme_pattern", EncodedExtremePatternItem::new);
    public static final RegistryObject<Item> PATTERN_CAPACITY_UPGRADE = ITEMS.register(
            "pattern_capacity_upgrade", PatternCapacityUpgradeItem::new);

    public static final RegistryObject<Item> MANA_DRILL_MODULE = ITEMS.register(
            "mana_drill_module", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MANA_DRILL_SPEED_UPGRADE = upgrade(
            "mana_drill_speed_upgrade", ManaDrillUpgradeItem.Type.SPEED, 1);
    public static final RegistryObject<Item> MANA_DRILL_SPEED_UPGRADE_2 = upgrade(
            "mana_drill_speed_upgrade_2", ManaDrillUpgradeItem.Type.SPEED, 2);
    public static final RegistryObject<Item> MANA_DRILL_SPEED_UPGRADE_3 = upgrade(
            "mana_drill_speed_upgrade_3", ManaDrillUpgradeItem.Type.SPEED, 3);
    public static final RegistryObject<Item> MANA_DRILL_SPEED_UPGRADE_4 = upgrade(
            "mana_drill_speed_upgrade_4", ManaDrillUpgradeItem.Type.SPEED, 4);
    public static final RegistryObject<Item> MANA_DRILL_SPEED_UPGRADE_5 = upgrade(
            "mana_drill_speed_upgrade_5", ManaDrillUpgradeItem.Type.SPEED, 5);

    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE = upgrade(
            "mana_drill_looting_upgrade", ManaDrillUpgradeItem.Type.LOOTING, 1);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_2 = upgrade(
            "mana_drill_looting_upgrade_2", ManaDrillUpgradeItem.Type.LOOTING, 2);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_3 = upgrade(
            "mana_drill_looting_upgrade_3", ManaDrillUpgradeItem.Type.LOOTING, 3);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_4 = upgrade(
            "mana_drill_looting_upgrade_4", ManaDrillUpgradeItem.Type.LOOTING, 4);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_5 = upgrade(
            "mana_drill_looting_upgrade_5", ManaDrillUpgradeItem.Type.LOOTING, 5);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_6 = upgrade(
            "mana_drill_looting_upgrade_6", ManaDrillUpgradeItem.Type.LOOTING, 6);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_7 = upgrade(
            "mana_drill_looting_upgrade_7", ManaDrillUpgradeItem.Type.LOOTING, 7);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_8 = upgrade(
            "mana_drill_looting_upgrade_8", ManaDrillUpgradeItem.Type.LOOTING, 8);
    public static final RegistryObject<Item> MANA_DRILL_LOOTING_UPGRADE_9 = upgrade(
            "mana_drill_looting_upgrade_9", ManaDrillUpgradeItem.Type.LOOTING, 9);

    public static final RegistryObject<Item> MANA_DRILL_GENERATION_UPGRADE = upgrade(
            "mana_drill_generation_upgrade", ManaDrillUpgradeItem.Type.GENERATION, 1);
    public static final RegistryObject<Item> MANA_DRILL_GENERATION_UPGRADE_2 = upgrade(
            "mana_drill_generation_upgrade_2", ManaDrillUpgradeItem.Type.GENERATION, 2);
    public static final RegistryObject<Item> MANA_DRILL_GENERATION_UPGRADE_3 = upgrade(
            "mana_drill_generation_upgrade_3", ManaDrillUpgradeItem.Type.GENERATION, 3);

    public static final RegistryObject<Item> GREENHOUSE_ECONOMY_MODULE_1 = greenhouseModule(
            "greenhouse_economy_module_1", GreenhouseModuleItem.Type.ECONOMY, 1);
    public static final RegistryObject<Item> GREENHOUSE_ECONOMY_MODULE_2 = greenhouseModule(
            "greenhouse_economy_module_2", GreenhouseModuleItem.Type.ECONOMY, 2);
    public static final RegistryObject<Item> GREENHOUSE_ECONOMY_MODULE_3 = greenhouseModule(
            "greenhouse_economy_module_3", GreenhouseModuleItem.Type.ECONOMY, 3);
    public static final RegistryObject<Item> GREENHOUSE_EFFICIENCY_MODULE_1 = greenhouseModule(
            "greenhouse_efficiency_module_1", GreenhouseModuleItem.Type.EFFICIENCY, 1);
    public static final RegistryObject<Item> GREENHOUSE_EFFICIENCY_MODULE_2 = greenhouseModule(
            "greenhouse_efficiency_module_2", GreenhouseModuleItem.Type.EFFICIENCY, 2);
    public static final RegistryObject<Item> GREENHOUSE_EFFICIENCY_MODULE_3 = greenhouseModule(
            "greenhouse_efficiency_module_3", GreenhouseModuleItem.Type.EFFICIENCY, 3);
    public static final RegistryObject<Item> GREENHOUSE_SPEED_MODULE_1 = greenhouseModule(
            "greenhouse_speed_module_1", GreenhouseModuleItem.Type.SPEED, 1);
    public static final RegistryObject<Item> GREENHOUSE_SPEED_MODULE_2 = greenhouseModule(
            "greenhouse_speed_module_2", GreenhouseModuleItem.Type.SPEED, 2);
    public static final RegistryObject<Item> GREENHOUSE_SPEED_MODULE_3 = greenhouseModule(
            "greenhouse_speed_module_3", GreenhouseModuleItem.Type.SPEED, 3);
    public static final RegistryObject<Item> GREENHOUSE_INFINITE_DAY_MODULE = greenhouseModule(
            "greenhouse_infinite_day_module", GreenhouseModuleItem.Type.INFINITE_DAY, 1);
    public static final RegistryObject<Item> GREENHOUSE_INFINITE_NIGHT_MODULE = greenhouseModule(
            "greenhouse_infinite_night_module", GreenhouseModuleItem.Type.INFINITE_NIGHT, 1);
    public static final RegistryObject<Item> GREENHOUSE_INFINITE_LAVA_MODULE = greenhouseModule(
            "greenhouse_infinite_lava_module", GreenhouseModuleItem.Type.INFINITE_LAVA, 1);

    private static RegistryObject<Item> blockItem(
            String name, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> upgrade(String name, ManaDrillUpgradeItem.Type type, int level) {
        return ITEMS.register(name, () -> new ManaDrillUpgradeItem(type, level));
    }

    private static RegistryObject<Item> greenhouseModule(
            String name, GreenhouseModuleItem.Type type, int level) {
        return ITEMS.register(name, () -> new GreenhouseModuleItem(type, level));
    }

    public static List<RegistryObject<Item>> manaDrillUpgradeItems() {
        return List.of(
                MANA_DRILL_SPEED_UPGRADE, MANA_DRILL_SPEED_UPGRADE_2, MANA_DRILL_SPEED_UPGRADE_3,
                MANA_DRILL_SPEED_UPGRADE_4, MANA_DRILL_SPEED_UPGRADE_5,
                MANA_DRILL_LOOTING_UPGRADE, MANA_DRILL_LOOTING_UPGRADE_2, MANA_DRILL_LOOTING_UPGRADE_3,
                MANA_DRILL_LOOTING_UPGRADE_4, MANA_DRILL_LOOTING_UPGRADE_5, MANA_DRILL_LOOTING_UPGRADE_6,
                MANA_DRILL_LOOTING_UPGRADE_7, MANA_DRILL_LOOTING_UPGRADE_8, MANA_DRILL_LOOTING_UPGRADE_9,
                MANA_DRILL_GENERATION_UPGRADE, MANA_DRILL_GENERATION_UPGRADE_2,
                MANA_DRILL_GENERATION_UPGRADE_3
        );
    }

    public static List<RegistryObject<Item>> greenhouseModuleItems() {
        return List.of(
                GREENHOUSE_ECONOMY_MODULE_1, GREENHOUSE_ECONOMY_MODULE_2, GREENHOUSE_ECONOMY_MODULE_3,
                GREENHOUSE_EFFICIENCY_MODULE_1, GREENHOUSE_EFFICIENCY_MODULE_2, GREENHOUSE_EFFICIENCY_MODULE_3,
                GREENHOUSE_SPEED_MODULE_1, GREENHOUSE_SPEED_MODULE_2, GREENHOUSE_SPEED_MODULE_3,
                GREENHOUSE_INFINITE_DAY_MODULE, GREENHOUSE_INFINITE_NIGHT_MODULE,
                GREENHOUSE_INFINITE_LAVA_MODULE
        );
    }

    public static List<RegistryObject<Item>> nativePatternItems() {
        return List.of(BLANK_EXTREME_PATTERN, ENCODED_EXTREME_PATTERN, PATTERN_CAPACITY_UPGRADE);
    }

    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private ModItems() {}
}
