package ru.rfvv.metatechreborn.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.recipe.GreenhouseRecipe;
import ru.rfvv.metatechreborn.recipe.GreenhouseRecipeSerializer;
import ru.rfvv.metatechreborn.recipe.ManaDrillRecipe;
import ru.rfvv.metatechreborn.recipe.ManaDrillRecipeSerializer;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipeSerializer;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MetaTechReborn.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MetaTechReborn.MOD_ID);

    public static final RegistryObject<RecipeType<MolecularAssemblerRecipe>> MOLECULAR_ASSEMBLING_TYPE =
            TYPES.register("molecular_assembling", () -> RecipeType.simple(
                    new ResourceLocation(MetaTechReborn.MOD_ID, "molecular_assembling")));
    public static final RegistryObject<RecipeSerializer<MolecularAssemblerRecipe>> MOLECULAR_ASSEMBLING_SERIALIZER =
            SERIALIZERS.register("molecular_assembling", MolecularAssemblerRecipeSerializer::new);

    public static final RegistryObject<RecipeType<ManaDrillRecipe>> MANA_DRILL_GENERATING_TYPE =
            TYPES.register("mana_drill_generating", () -> RecipeType.simple(
                    new ResourceLocation(MetaTechReborn.MOD_ID, "mana_drill_generating")));
    public static final RegistryObject<RecipeSerializer<ManaDrillRecipe>> MANA_DRILL_GENERATING_SERIALIZER =
            SERIALIZERS.register("mana_drill_generating", ManaDrillRecipeSerializer::new);

    public static final RegistryObject<RecipeType<GreenhouseRecipe>> GREENHOUSE_TYPE =
            TYPES.register("greenhouse", () -> RecipeType.simple(
                    new ResourceLocation(MetaTechReborn.MOD_ID, "greenhouse")));
    public static final RegistryObject<RecipeSerializer<GreenhouseRecipe>> GREENHOUSE_SERIALIZER =
            SERIALIZERS.register("greenhouse", GreenhouseRecipeSerializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
    private ModRecipes() {}
}
