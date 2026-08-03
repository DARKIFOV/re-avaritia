package ru.rfvv.metatechreborn;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import ru.rfvv.metatechreborn.config.CommonConfig;
import ru.rfvv.metatechreborn.registry.ModBlockEntities;
import ru.rfvv.metatechreborn.registry.ModBlocks;
import ru.rfvv.metatechreborn.registry.ModItems;
import ru.rfvv.metatechreborn.registry.ModMenus;
import ru.rfvv.metatechreborn.registry.ModRecipes;

@Mod(MetaTechReborn.MOD_ID)
public final class MetaTechReborn {
    public static final String MOD_ID = "metatech_reborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MetaTechReborn() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.MOLECULAR_ASSEMBLER_9X9.get());
            event.accept(ModItems.NEUTRONIUM_COMBINER.get());
            event.accept(ModItems.MANA_DRILL.get());
            event.accept(ModItems.MANA_DRILL_CASING.get());
            event.accept(ModItems.MANA_DRILL_CORE.get());
            event.accept(ModItems.MANA_DRILL_NOZZLE.get());
            event.accept(ModItems.MANA_DRILL_MODULE.get());
            event.accept(ModItems.GREENHOUSE.get());
            ModItems.manaDrillUpgradeItems().forEach(item -> event.accept(item.get()));
            ModItems.neutronCombinerUpgradeItems().forEach(item -> event.accept(item.get()));
            ModItems.greenhouseModuleItems().forEach(item -> event.accept(item.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            ModItems.electricSwordItems().forEach(item -> event.accept(item.get()));
            event.accept(ModItems.SNOW_GUN.get());
            event.accept(ModItems.SKULL_AXE.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.META_VAJRA.get());
            ModItems.windRotorItems().forEach(item -> event.accept(item.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            ModItems.energyFoodItems().forEach(item -> event.accept(item.get()));
        }
    }
}
