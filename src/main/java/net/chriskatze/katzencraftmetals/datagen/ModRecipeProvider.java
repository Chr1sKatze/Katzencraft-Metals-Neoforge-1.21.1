package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.datagen.recipe.CrusherRecipeBuilder;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // =========================
        // SMELTING / BLASTING
        // =========================

        oreSmelting(recipeOutput, ModItems.RAW_PLATINUM.get(), RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get(), 0.7f, 200, "platinum");
        oreBlasting(recipeOutput, ModItems.RAW_PLATINUM.get(), RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get(), 0.7f, 100, "platinum");

        oreSmelting(recipeOutput, ModItems.RAW_MYTHRIL.get(), RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get(), 0.7f, 200, "mythril");
        oreBlasting(recipeOutput, ModItems.RAW_MYTHRIL.get(), RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get(), 0.7f, 100, "mythril");

        // =========================
        // BLOCK <-> INGOT
        // =========================

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLATINUM_BLOCK.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.PLATINUM_INGOT.get())
                .unlockedBy("has_platinum_ingot", has(ModItems.PLATINUM_INGOT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get(), 9)
                .requires(ModBlocks.PLATINUM_BLOCK.get())
                .unlockedBy("has_platinum_block", has(ModBlocks.PLATINUM_BLOCK.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_ingot_from_block");

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MYTHRIL_BLOCK.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get(), 9)
                .requires(ModBlocks.MYTHRIL_BLOCK.get())
                .unlockedBy("has_mythril_block", has(ModBlocks.MYTHRIL_BLOCK.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_ingot_from_block");

        // =========================
        // CRUSHER
        // =========================

        CrusherRecipeBuilder.crushing(
                ModItems.RAW_PLATINUM.get(),
                ModItems.PLATINUM_POWDER.get(), 1)
                    .processingTime((100))
                    .secondOutput(Items.GOLD_NUGGET, 1, 0.25f)
                    .thirdOutput(Items.DIAMOND, 1, 0.10f)
                    .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/raw_platinum")
        );

        CrusherRecipeBuilder.crushing(
                ModItems.RAW_MYTHRIL.get(),
                ModItems.MYTHRIL_POWDER.get(), 2)
                    .processingTime(300)
                    .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/raw_mythril")
        );

        // =========================
        // INGOT <-> NUGGET
        // =========================

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModItems.PLATINUM_NUGGET.get())
                .unlockedBy("has_platinum_nugget", has(ModItems.PLATINUM_NUGGET.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_ingot_from_nuggets");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLATINUM_NUGGET.get(), 9)
                .requires(ModItems.PLATINUM_INGOT.get())
                .unlockedBy("has_platinum_ingot", has(ModItems.PLATINUM_INGOT.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_nugget_from_ingot");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModItems.MYTHRIL_NUGGET.get())
                .unlockedBy("has_mythril_nugget", has(ModItems.MYTHRIL_NUGGET.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_ingot_from_nuggets");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MYTHRIL_NUGGET.get(), 9)
                .requires(ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_nugget_from_ingot");
    }

    protected static void oreSmelting(RecipeOutput recipeOutput, net.minecraft.world.item.Item input, RecipeCategory category,
                                      net.minecraft.world.item.Item result, float experience, int cookingTime, String group) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(input), category, result, experience, cookingTime, net.minecraft.world.item.crafting.RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new)
                .group(group)
                .unlockedBy("has_" + input.builtInRegistryHolder().key().location().getPath(), has(input))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":" + result.builtInRegistryHolder().key().location().getPath() + "_from_smelting_" + input.builtInRegistryHolder().key().location().getPath());
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, net.minecraft.world.item.Item input, RecipeCategory category,
                                      net.minecraft.world.item.Item result, float experience, int cookingTime, String group) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(input), category, result, experience, cookingTime, net.minecraft.world.item.crafting.RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new)
                .group(group)
                .unlockedBy("has_" + input.builtInRegistryHolder().key().location().getPath(), has(input))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":" + result.builtInRegistryHolder().key().location().getPath() + "_from_blasting_" + input.builtInRegistryHolder().key().location().getPath());
    }
}