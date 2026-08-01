package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.block.ManaDrillBlock;
import ru.rfvv.metatechreborn.block.MolecularAssemblerBlock;
import ru.rfvv.metatechreborn.block.NeutroniumCombinerBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Block> MOLECULAR_ASSEMBLER_9X9 = BLOCKS.register(
            "molecular_assembler_9x9",
            () -> new MolecularAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NEUTRONIUM_COMBINER = BLOCKS.register(
            "neutronium_combiner",
            () -> new NeutroniumCombinerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MANA_DRILL = BLOCKS.register(
            "mana_drill",
            () -> new ManaDrillBlock(BlockBehaviour.Properties.copy(Blocks.REINFORCED_DEEPSLATE)
                    .strength(7.0F, 18.0F).requiresCorrectToolForDrops()));

    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    private ModBlocks() {}
}
