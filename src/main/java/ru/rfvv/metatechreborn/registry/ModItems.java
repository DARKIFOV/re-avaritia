package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.item.ElectricSwordItem;
import ru.rfvv.metatechreborn.item.EnergyFoodItem;
import ru.rfvv.metatechreborn.item.ManaDrillUpgradeItem;
import ru.rfvv.metatechreborn.item.MetaVajraItem;
import ru.rfvv.metatechreborn.item.NeutroniumCombinerUpgradeItem;
import ru.rfvv.metatechreborn.item.SkullAxeItem;
import ru.rfvv.metatechreborn.item.SnowGunItem;
import ru.rfvv.metatechreborn.item.WindRotorItem;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Item> MOLECULAR_ASSEMBLER_9X9 = blockItem(
            "molecular_assembler_9x9", ModBlocks.MOLECULAR_ASSEMBLER_9X9);
    public static final RegistryObject<Item> NEUTRONIUM_COMBINER = blockItem(
            "neutronium_combiner", ModBlocks.NEUTRONIUM_COMBINER);
    public static final RegistryObject<Item> MANA_DRILL = blockItem(
            "mana_drill", ModBlocks.MANA_DRILL);
    public static final RegistryObject<Item> MANA_DRILL_CASING = blockItem(
            "mana_drill_casing", ModBlocks.MANA_DRILL_CASING);
    public static final RegistryObject<Item> MANA_DRILL_CORE = blockItem(
            "mana_drill_core", ModBlocks.MANA_DRILL_CORE);
    public static final RegistryObject<Item> MANA_DRILL_NOZZLE = blockItem(
            "mana_drill_nozzle", ModBlocks.MANA_DRILL_NOZZLE);

    public static final RegistryObject<Item> NEUTRON_COMBINER_SPEED_UPGRADE = ITEMS.register(
            "neutron_combiner_speed_upgrade",
            () -> new NeutroniumCombinerUpgradeItem(NeutroniumCombinerUpgradeItem.Type.SPEED));
    public static final RegistryObject<Item> NEUTRON_COMBINER_EFFICIENCY_UPGRADE = ITEMS.register(
            "neutron_combiner_efficiency_upgrade",
            () -> new NeutroniumCombinerUpgradeItem(NeutroniumCombinerUpgradeItem.Type.EFFICIENCY));
    public static final RegistryObject<Item> NEUTRON_COMBINER_OUTPUT_UPGRADE = ITEMS.register(
            "neutron_combiner_output_upgrade",
            () -> new NeutroniumCombinerUpgradeItem(NeutroniumCombinerUpgradeItem.Type.OUTPUT));

    public static final RegistryObject<Item> MANA_DRILL_MODULE = ITEMS.register(
            "mana_drill_module", () -> new Item(new Item.Properties().stacksTo(1)));

    // The original pack configuration contains 5 speed, 9 looting and 3 generation tiers.
    // Tier-one IDs are retained from MVP3 so existing worlds migrate without missing items.
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

    // Restored item batches from MetaAdvanced and MetaThaumcraft.
    public static final RegistryObject<Item> META_VAJRA = ITEMS.register("meta_vajra", MetaVajraItem::new);
    public static final RegistryObject<Item> SNOW_GUN = ITEMS.register("snow_gun", SnowGunItem::new);
    public static final RegistryObject<Item> SKULL_AXE = ITEMS.register("skull_axe", SkullAxeItem::new);

    public static final RegistryObject<Item> ENERGY_FOOD_TIER_1 = energyFood(
            "energy_food_tier_1", 1, 1.0F, 32, 150, 50_000, 500, 1);
    public static final RegistryObject<Item> ENERGY_FOOD_TIER_2 = energyFood(
            "energy_food_tier_2", 2, 2.0F, 16, 300, 100_000, 1_000, 2);
    public static final RegistryObject<Item> ENERGY_FOOD_TIER_3 = energyFood(
            "energy_food_tier_3", 3, 3.0F, 1, 450, 150_000, 1_500, 3);
    public static final RegistryObject<Item> ENERGY_FOOD_TIER_4 = energyFood(
            "energy_food_tier_4", 20, 20.0F, 0, 600, 300_000, 3_000, 4);

    public static final RegistryObject<Item> WIND_ROTOR_IRIDIUM = windRotor(
            "item_wind_iridium_rotor", 11, 1_209_600, 320.0F, 10.0D, 120.0D);
    public static final RegistryObject<Item> WIND_ROTOR_QUANTUM = windRotor(
            "item_wind_quantum_rotor", 11, 2_419_200, 640.0F, 10.0D, 120.0D);
    public static final RegistryObject<Item> WIND_ROTOR_ULTIMATE = windRotor(
            "item_wind_ultimate_rotor", 11, 4_838_400, 1_280.0F, 10.0D, 120.0D);

    public static final RegistryObject<Item> ELECTRIC_SWORD_REALMITE = electricSword(
            "electric_sword_realmite", 180_000, 550, 22.0F, 400, 70);
    public static final RegistryObject<Item> ELECTRIC_SWORD_ARLEMITE = electricSword(
            "electric_sword_arlemite", 200_000, 600, 24.0F, 400, 76);
    public static final RegistryObject<Item> ELECTRIC_SWORD_RUPIUM = electricSword(
            "electric_sword_rupium", 220_000, 650, 26.0F, 400, 82);
    public static final RegistryObject<Item> ELECTRIC_SWORD_EDEM = electricSword(
            "electric_sword_edem", 160_000, 700, 28.0F, 400, 64);
    public static final RegistryObject<Item> ELECTRIC_SWORD_WILDFOREST = electricSword(
            "electric_sword_wildforest", 200_000, 750, 30.0F, 400, 80);
    public static final RegistryObject<Item> ELECTRIC_SWORD_APALACHI = electricSword(
            "electric_sword_apalachi", 240_000, 800, 33.0F, 400, 96);
    public static final RegistryObject<Item> ELECTRIC_SWORD_SKYUNDER = electricSword(
            "electric_sword_skyunder", 280_000, 850, 35.0F, 400, 112);
    public static final RegistryObject<Item> ELECTRIC_SWORD_MORTUM = electricSword(
            "electric_sword_mortum", 320_000, 900, 37.0F, 400, 128);
    public static final RegistryObject<Item> ELECTRIC_SWORD_CHALITE = electricSword(
            "electric_sword_chalite", 360_000, 950, 40.0F, 400, 144);
    public static final RegistryObject<Item> ELECTRIC_SWORD_ADMIN = electricSword(
            "electric_sword_admin", Integer.MAX_VALUE, Integer.MAX_VALUE, 2_048.0F, 1, 1);

    private static RegistryObject<Item> blockItem(String name, RegistryObject<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> upgrade(String name, ManaDrillUpgradeItem.Type type, int level) {
        return ITEMS.register(name, () -> new ManaDrillUpgradeItem(type, level));
    }

    private static RegistryObject<Item> energyFood(String name, int nutrition, float saturation, int useDuration,
                                                   int energyCost, int capacity, int transferLimit, int tier) {
        return ITEMS.register(name, () -> new EnergyFoodItem(
                nutrition, saturation, useDuration, energyCost, capacity, transferLimit, tier));
    }

    private static RegistryObject<Item> windRotor(String name, int radius, int durability, float efficiency,
                                                  double minWindStrength, double maxWindStrength) {
        return ITEMS.register(name, () -> new WindRotorItem(
                radius, durability, efficiency, minWindStrength, maxWindStrength));
    }

    private static RegistryObject<Item> electricSword(String name, int capacity, int transferLimit,
                                                       float activeDamage, int hitCost, int passiveCost) {
        return ITEMS.register(name,
                () -> new ElectricSwordItem(capacity, transferLimit, activeDamage, hitCost, passiveCost));
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

    public static List<RegistryObject<Item>> neutronCombinerUpgradeItems() {
        return List.of(NEUTRON_COMBINER_SPEED_UPGRADE,
                NEUTRON_COMBINER_EFFICIENCY_UPGRADE,
                NEUTRON_COMBINER_OUTPUT_UPGRADE);
    }

    public static List<RegistryObject<Item>> electricSwordItems() {
        return List.of(
                ELECTRIC_SWORD_REALMITE, ELECTRIC_SWORD_ARLEMITE, ELECTRIC_SWORD_RUPIUM,
                ELECTRIC_SWORD_EDEM, ELECTRIC_SWORD_WILDFOREST, ELECTRIC_SWORD_APALACHI,
                ELECTRIC_SWORD_SKYUNDER, ELECTRIC_SWORD_MORTUM, ELECTRIC_SWORD_CHALITE,
                ELECTRIC_SWORD_ADMIN
        );
    }

    public static List<RegistryObject<Item>> energyFoodItems() {
        return List.of(ENERGY_FOOD_TIER_1, ENERGY_FOOD_TIER_2, ENERGY_FOOD_TIER_3, ENERGY_FOOD_TIER_4);
    }

    public static List<RegistryObject<Item>> windRotorItems() {
        return List.of(WIND_ROTOR_IRIDIUM, WIND_ROTOR_QUANTUM, WIND_ROTOR_ULTIMATE);
    }

    public static List<RegistryObject<Item>> portedToolItems() {
        return List.of(META_VAJRA, SNOW_GUN, SKULL_AXE);
    }

    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private ModItems() {}
}
