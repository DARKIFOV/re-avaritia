package ru.rfvv.metatechreborn.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.block.MysticalInfusionAssemblerBlock;
import ru.rfvv.metatechreborn.block.MysticalInfusionEncoderBlock;
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionAssemblerBlockEntity;
import ru.rfvv.metatechreborn.blockentity.MysticalInfusionEncoderBlockEntity;
import ru.rfvv.metatechreborn.item.BlankMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.item.EncodedMysticalInfusionPatternItem;
import ru.rfvv.metatechreborn.menu.MysticalInfusionAssemblerMenu;
import ru.rfvv.metatechreborn.menu.MysticalInfusionEncoderMenu;

public final class ModMysticalInfusion {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MetaTechReborn.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MetaTechReborn.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MetaTechReborn.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MetaTechReborn.MOD_ID);

    public static final RegistryObject<Block> ASSEMBLER = BLOCKS.register("mystical_infusion_assembler",
            () -> new MysticalInfusionAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ENCODER = BLOCKS.register("mystical_infusion_encoder",
            () -> new MysticalInfusionEncoderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 12.0F).requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> ASSEMBLER_ITEM = ITEMS.register("mystical_infusion_assembler",
            () -> new BlockItem(ASSEMBLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> ENCODER_ITEM = ITEMS.register("mystical_infusion_encoder",
            () -> new BlockItem(ENCODER.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLANK_PATTERN = ITEMS.register("blank_mystical_infusion_pattern",
            BlankMysticalInfusionPatternItem::new);
    public static final RegistryObject<Item> ENCODED_PATTERN = ITEMS.register("encoded_mystical_infusion_pattern",
            EncodedMysticalInfusionPatternItem::new);

    public static final RegistryObject<BlockEntityType<MysticalInfusionAssemblerBlockEntity>> ASSEMBLER_BE =
            BLOCK_ENTITIES.register("mystical_infusion_assembler", () -> BlockEntityType.Builder.of(
                    MysticalInfusionAssemblerBlockEntity::new, ASSEMBLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MysticalInfusionEncoderBlockEntity>> ENCODER_BE =
            BLOCK_ENTITIES.register("mystical_infusion_encoder", () -> BlockEntityType.Builder.of(
                    MysticalInfusionEncoderBlockEntity::new, ENCODER.get()).build(null));

    public static final RegistryObject<MenuType<MysticalInfusionAssemblerMenu>> ASSEMBLER_MENU =
            MENUS.register("mystical_infusion_assembler", () -> IForgeMenuType.create(MysticalInfusionAssemblerMenu::new));
    public static final RegistryObject<MenuType<MysticalInfusionEncoderMenu>> ENCODER_MENU =
            MENUS.register("mystical_infusion_encoder", () -> IForgeMenuType.create(MysticalInfusionEncoderMenu::new));

    private ModMysticalInfusion() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
