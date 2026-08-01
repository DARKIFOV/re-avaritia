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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("molecular_assembler_9x9");
        ENABLE_AVARITIA_INTEGRATION = builder.define("enableAvaritiaIntegration", true);
        ASSEMBLER_CAPACITY = builder.defineInRange("energyCapacity", 2_000_000, 1_000, Integer.MAX_VALUE);
        ASSEMBLER_MAX_RECEIVE = builder.defineInRange("maxReceive", 20_000, 1, Integer.MAX_VALUE);
        DEFAULT_CRAFT_TIME = builder.defineInRange("defaultCraftTime", 400, 1, 72_000);
        DEFAULT_ENERGY_PER_TICK = builder.defineInRange("defaultEnergyPerTick", 500, 0, Integer.MAX_VALUE);
        AUTO_EJECT_OUTPUT = builder.define("autoEjectOutput", true);
        builder.pop();

        builder.push("neutronium_combiner");
        NEUTRON_COMBINER_CAPACITY = builder.defineInRange("energyCapacity", 5_000_000, 1_000, Integer.MAX_VALUE);
        NEUTRON_COMBINER_MAX_RECEIVE = builder.defineInRange("maxReceive", 50_000, 1, Integer.MAX_VALUE);
        NEUTRON_COMBINER_AUTO_EJECT = builder.define("autoEjectOutput", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CommonConfig() {
    }
}
