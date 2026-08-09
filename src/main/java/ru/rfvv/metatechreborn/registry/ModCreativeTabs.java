package ru.rfvv.metatechreborn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MetaTechReborn.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "metatech_reborn",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("MetaTech Reborn"))
                    .icon(() -> ModItems.MOLECULAR_ASSEMBLER_9X9.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (Item item : ForgeRegistries.ITEMS.getValues()) {
                            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                            if (id != null && MetaTechReborn.MOD_ID.equals(id.getNamespace())) {
                                output.accept(item);
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
