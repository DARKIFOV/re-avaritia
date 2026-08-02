package ru.rfvv.metatechreborn.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AVARITIA_INTEGRATION;
    public static final ForgeConfigSpec.IntValue ASSEMBLER_CAPACITY;
    public static final ForgeConfigSpec.IntValue ASSEMBLER_MAX_RECEIVE;
    public static final ForgeConfigSpec.IntValue DEFAULT_CRAFT_TIME;
    public static final ForgeConfigSpec.IntValue DEFAULT_ENERGY_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue AUTO_EJECT_OUTPUT;

    public static final ForgeConfigSpec.IntValue NEUTRON_COMBINER_CAPACITY;
    public static final ForgeConfigSpec.IntValue NEUTRON_COMBINER_MAX_RECEIVE;
    public static final ForgeConfigSpec.BooleanValue NEUTRON_COMBINER_AUTO_EJECT;

    public static final ForgeConfigSpec.IntValue LUCK_CONVERTER_CAPACITY;
    public static final ForgeConfigSpec.IntValue LUCK_CONVERTER_MAX_RECEIVE;
    public static final ForgeConfigSpec.IntValue LUCK_CONVERTER_ENERGY_PER_TICK;
    public static final ForgeConfigSpec.IntValue LUCK_CONVERTER_TIME;
    public static final ForgeConfigSpec.IntValue ADVANCED_LUCK_CONVERTER_TIME;

    public static final ForgeConfigSpec.IntValue MANA_DRILL_MANA_CAPACITY;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_POOL_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_POOL_SCAN_INTERVAL;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_MAX_POOL_TRANSFER;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_MAX_SPEED_UPGRADES;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_MAX_LOOTING_UPGRADES;
    public static final ForgeConfigSpec.IntValue MANA_DRILL_MAX_GENERATION_UPGRADES;
    public static final ForgeConfigSpec.BooleanValue MANA_DRILL_AUTO_EJECT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("extreme_molecular_assembler_9x9");
        ENABLE_AVARITIA_INTEGRATION = builder.define("enableAvaritiaIntegration", true);
        ASSEMBLER_CAPACITY = builder.defineInRange("energyCapacity", 2_000_000, 1_000, Integer.MAX_VALUE);
        ASSEMBLER_MAX_RECEIVE = builder.defineInRange("maxReceive", 20_000, 1, Integer.MAX_VALUE);
        DEFAULT_CRAFT_TIME = builder.comment("MetaAdvanced AE2AvaritiaExtremeAssembler OperationLength.")
                .defineInRange("defaultCraftTime", 24, 1, 72_000);
        DEFAULT_ENERGY_PER_TICK = builder.comment("MetaAdvanced extreme assembler base energy consumption.")
                .defineInRange("defaultEnergyPerTick", 100, 0, Integer.MAX_VALUE);
        AUTO_EJECT_OUTPUT = builder.define("autoEjectOutput", true);
        builder.pop();

        builder.push("neutronium_combiner");
        NEUTRON_COMBINER_CAPACITY = builder.defineInRange("energyCapacity", 10_000_000, 1_000, Integer.MAX_VALUE);
        NEUTRON_COMBINER_MAX_RECEIVE = builder.defineInRange("maxReceive", 100_000, 1, Integer.MAX_VALUE);
        NEUTRON_COMBINER_AUTO_EJECT = builder.define("autoEjectOutput", true);
        builder.pop();

        builder.push("luck_converters");
        LUCK_CONVERTER_CAPACITY = builder.comment("Original MetaAdvanced internal FE buffer was 10000.")
                .defineInRange("energyCapacity", 10_000, 1_000, Integer.MAX_VALUE);
        LUCK_CONVERTER_MAX_RECEIVE = builder.defineInRange("maxReceive", 1_000, 1, Integer.MAX_VALUE);
        LUCK_CONVERTER_ENERGY_PER_TICK = builder.comment("Original base consumption was 100 energy.")
                .defineInRange("energyPerTick", 100, 1, Integer.MAX_VALUE);
        LUCK_CONVERTER_TIME = builder.comment("Original basic operation length.")
                .defineInRange("basicOperationTicks", 1_000, 1, 72_000);
        ADVANCED_LUCK_CONVERTER_TIME = builder.comment("Original advanced coefficient 0.5 of 1000 ticks.")
                .defineInRange("advancedOperationTicks", 500, 1, 72_000);
        builder.pop();

        builder.push("mana_drill");
        MANA_DRILL_MANA_CAPACITY = builder.comment("Internal Botania mana buffer.")
                .defineInRange("manaCapacity", 10_000_000, 1_000, Integer.MAX_VALUE);
        MANA_DRILL_POOL_SCAN_RADIUS = builder.defineInRange("poolScanRadius", 10, 1, 32);
        MANA_DRILL_POOL_SCAN_INTERVAL = builder.defineInRange("poolScanInterval", 40, 1, 1_200);
        MANA_DRILL_MAX_POOL_TRANSFER = builder.defineInRange("maxPoolTransfer", 1_000_000, 1, Integer.MAX_VALUE);
        MANA_DRILL_MAX_SPEED_UPGRADES = builder.defineInRange("maxSpeedUpgrades", 5, 0, 64);
        MANA_DRILL_MAX_LOOTING_UPGRADES = builder.defineInRange("maxLootingUpgrades", 9, 0, 64);
        MANA_DRILL_MAX_GENERATION_UPGRADES = builder.defineInRange("maxGenerationUpgrades", 3, 0, 64);
        MANA_DRILL_AUTO_EJECT = builder.define("autoEjectOutput", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CommonConfig() {}
}
