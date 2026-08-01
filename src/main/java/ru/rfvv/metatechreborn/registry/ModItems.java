package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Item> MOLECULAR_ASSEMBLER_9X9 = ITEMS.register(
            "molecular_assembler_9x9",
            () -> new BlockItem(ModBlocks.MOLECULAR_ASSEMBLER_9X9.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> NEUTRONIUM_COMBINER = ITEMS.register(
            "neutronium_combiner",
            () -> new BlockItem(ModBlocks.NEUTRONIUM_COMBINER.get(), new Item.Properties())
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
