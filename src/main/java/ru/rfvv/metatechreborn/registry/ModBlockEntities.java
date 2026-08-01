package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.blockentity.ManaDrillBlockEntity;
import ru.rfvv.metatechreborn.blockentity.MolecularAssemblerBlockEntity;
import ru.rfvv.metatechreborn.blockentity.NeutroniumCombinerBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MetaTechReborn.MOD_ID);

    public static final RegistryObject<BlockEntityType<MolecularAssemblerBlockEntity>> MOLECULAR_ASSEMBLER_9X9 =
            BLOCK_ENTITIES.register("molecular_assembler_9x9", () -> BlockEntityType.Builder.of(
                    MolecularAssemblerBlockEntity::new, ModBlocks.MOLECULAR_ASSEMBLER_9X9.get()).build(null));
    public static final RegistryObject<BlockEntityType<NeutroniumCombinerBlockEntity>> NEUTRONIUM_COMBINER =
            BLOCK_ENTITIES.register("neutronium_combiner", () -> BlockEntityType.Builder.of(
                    NeutroniumCombinerBlockEntity::new, ModBlocks.NEUTRONIUM_COMBINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ManaDrillBlockEntity>> MANA_DRILL =
            BLOCK_ENTITIES.register("mana_drill", () -> BlockEntityType.Builder.of(
                    ManaDrillBlockEntity::new, ModBlocks.MANA_DRILL.get()).build(null));

    public static void register(IEventBus bus) { BLOCK_ENTITIES.register(bus); }
    private ModBlockEntities() {}
}
