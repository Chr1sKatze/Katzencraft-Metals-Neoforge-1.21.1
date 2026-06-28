package net.chriskatze.katzencraftmetals.recipe;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    Registries.RECIPE_SERIALIZER,
                    KatzencraftMetalsMod.MODID
            );

    public static final DeferredRegister<RecipeType<?>>
            RECIPE_TYPES =
            DeferredRegister.create(
                    Registries.RECIPE_TYPE,
                    KatzencraftMetalsMod.MODID
            );

    // =========================
    // CRUSHER
    // =========================

    public static final DeferredHolder<
            RecipeSerializer<?>,
            RecipeSerializer<CrusherRecipe>
            > CRUSHER_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "crusher",
                    CrusherRecipe.Serializer::new
            );

    public static final DeferredHolder<
            RecipeType<?>,
            RecipeType<CrusherRecipe>
            > CRUSHER_TYPE =
            RECIPE_TYPES.register(
                    "crusher",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return KatzencraftMetalsMod.MODID
                                    + ":crusher";
                        }
                    }
            );

    // =========================
    // FOUNDRY MELTING
    // =========================

    public static final DeferredHolder<
            RecipeSerializer<?>,
            RecipeSerializer<FoundryMeltingRecipe>
            > FOUNDRY_MELTING_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "foundry_melting",
                    FoundryMeltingRecipe.Serializer::new
            );

    public static final DeferredHolder<
            RecipeType<?>,
            RecipeType<FoundryMeltingRecipe>
            > FOUNDRY_MELTING_TYPE =
            RECIPE_TYPES.register(
                    "foundry_melting",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return KatzencraftMetalsMod.MODID
                                    + ":foundry_melting";
                        }
                    }
            );

    private ModRecipes() {
    }
}
