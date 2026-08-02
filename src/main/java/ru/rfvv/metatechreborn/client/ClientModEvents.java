package ru.rfvv.metatechreborn.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.client.screen.ExtremePatternEncoderScreen;
import ru.rfvv.metatechreborn.client.screen.GreenhouseScreen;
import ru.rfvv.metatechreborn.client.screen.LuckConverterScreen;
import ru.rfvv.metatechreborn.client.screen.ManaDrillScreen;
import ru.rfvv.metatechreborn.client.screen.MolecularAssemblerScreen;
import ru.rfvv.metatechreborn.client.screen.NeutroniumCombinerScreen;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModMenus;

@Mod.EventBusSubscriber(modid = MetaTechReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.MOLECULAR_ASSEMBLER_9X9.get(), MolecularAssemblerScreen::new);
            MenuScreens.register(ModMenus.EXTREME_PATTERN_ENCODER.get(), ExtremePatternEncoderScreen::new);
            MenuScreens.register(ModMenus.NEUTRONIUM_COMBINER.get(), NeutroniumCombinerScreen::new);
            MenuScreens.register(ModMenus.LUCK_CONVERTER.get(), LuckConverterScreen::new);
            MenuScreens.register(ModMenus.MANA_DRILL.get(), ManaDrillScreen::new);
            MenuScreens.register(ModMenus.GREENHOUSE.get(), GreenhouseScreen::new);

            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MOLECULAR_ASSEMBLER_9X9.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GREENHOUSE.get(), RenderType.translucent());
        });
    }
    private ClientModEvents() {}
}
