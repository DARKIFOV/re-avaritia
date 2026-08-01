package ru.rfvv.metatechreborn.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.rfvv.metatechreborn.MetaTechReborn;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipe;
import ru.rfvv.metatechreborn.recipe.MolecularAssemblerRecipeSerializer;
import ru.rfvv.metatechreborn.recipe.NeutroniumCombinerRecipe;
import ru.rfvv.metatechreborn.recipe.NeutroniumCombinerRecipeSerializer;

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

    public static final RegistryObject<RecipeType<NeutroniumCombinerRecipe>> NEUTRONIUM_COMBINING_TYPE =
            TYPES.register("neutronium_combining", () -> RecipeType.simple(
                    new ResourceLocation(MetaTechReborn.MOD_ID, "neutronium_combining")));

    public static final RegistryObject<RecipeSerializer<NeutroniumCombinerRecipe>> NEUTRONIUM_COMBINING_SERIALIZER =
            SERIALIZERS.register("neutronium_combining", NeutroniumCombinerRecipeSerializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModRecipes() {
    }
}
