package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;

/** Items belonging to the native 9x9 pattern terminal. */
public final class ModPatternItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Item> EXTREME_PATTERN_ENCODER = ITEMS.register(
            "extreme_pattern_encoder",
            () -> new BlockItem(ModBlocks.EXTREME_PATTERN_ENCODER.get(), new Item.Properties()));

    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private ModPatternItems() {}
}
