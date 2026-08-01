package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.menu.MolecularAssemblerMenu;
import ru.rfvv.metatechreborn.menu.NeutroniumCombinerMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MetaTechReborn.MOD_ID);

    public static final RegistryObject<MenuType<MolecularAssemblerMenu>> MOLECULAR_ASSEMBLER_9X9 =
            MENUS.register("molecular_assembler_9x9", () -> IForgeMenuType.create(MolecularAssemblerMenu::new));

    public static final RegistryObject<MenuType<NeutroniumCombinerMenu>> NEUTRONIUM_COMBINER =
            MENUS.register("neutronium_combiner", () -> IForgeMenuType.create(NeutroniumCombinerMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {
    }
}
