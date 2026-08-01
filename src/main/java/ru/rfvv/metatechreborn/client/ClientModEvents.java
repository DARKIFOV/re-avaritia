package ru.rfvv.metatechreborn.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.client.screen.MolecularAssemblerScreen;
import ru.rfvv.metatechreborn.client.screen.NeutroniumCombinerScreen;
import ru.rfvv.metatechreborn.registry.ModMenus;

@Mod.EventBusSubscriber(modid = MetaTechReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.MOLECULAR_ASSEMBLER_9X9.get(), MolecularAssemblerScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.NEUTRONIUM_COMBINER.get(), NeutroniumCombinerScreen::new));
    }
    private ClientModEvents() {}
}
