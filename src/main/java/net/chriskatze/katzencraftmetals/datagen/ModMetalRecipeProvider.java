package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.datagen.recipe.CrusherRecipeBuilder;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Datagen-only recipes for metal material progression.
 *
 * This keeps repeated metal recipe sets out of ModRecipeProvider:
 * - crushed ore blast furnace fallbacks
 * - metal block <-> ingot recipes
 * - ingot <-> nugget recipes
 * - crusher raw-material recipes
 */
public final class ModMetalRecipeProvider {

    private ModMetalRecipeProvider() {
    }

    public static void build(
            RecipeOutput recipeOutput
    ) {
        buildBlastFurnaceRecipes(
                recipeOutput
        );

        buildBlockAndIngotRecipes(
                recipeOutput
        );

        buildCrusherRecipes(
                recipeOutput
        );

        buildIngotAndNuggetRecipes(
                recipeOutput
        );
    }

    private static void buildBlastFurnaceRecipes(
            RecipeOutput recipeOutput
    ) {
        oreBlasting(
                recipeOutput,
                ModItems.CRUSHED_PLATINUM_ORE.get(),
                RecipeCategory.MISC,
                ModItems.PLATINUM_NUGGET.get(),
                0.0f,
                100,
                "platinum"
        );

        oreBlasting(
                recipeOutput,
                ModItems.CRUSHED_MYTHRIL_ORE.get(),
                RecipeCategory.MISC,
                ModItems.MYTHRIL_NUGGET.get(),
                0.0f,
                100,
                "mythril"
        );

        oreBlasting(
                recipeOutput,
                ModItems.STEEL_CHARGE.get(),
                RecipeCategory.MISC,
                ModItems.STEEL_NUGGET.get(),
                0.0f,
                100,
                "steel"
        );
    }

    private static void buildBlockAndIngotRecipes(
            RecipeOutput recipeOutput
    ) {
        blockAndIngot(
                recipeOutput,
                "steel",
                ModBlocks.STEEL_BLOCK.get(),
                ModItems.STEEL_INGOT.get()
        );

        blockAndIngot(
                recipeOutput,
                "platinum",
                ModBlocks.PLATINUM_BLOCK.get(),
                ModItems.PLATINUM_INGOT.get()
        );

        blockAndIngot(
                recipeOutput,
                "mythril",
                ModBlocks.MYTHRIL_BLOCK.get(),
                ModItems.MYTHRIL_INGOT.get()
        );
    }

    private static void buildCrusherRecipes(
            RecipeOutput recipeOutput
    ) {
        CrusherRecipeBuilder.crushing(
                        Items.COAL,
                        ModItems.CRUSHED_COAL.get(),
                        3
                )
                .processingTime(
                        300
                )
                .save(
                        recipeOutput,
                        id(
                                "crusher/coal"
                        )
                );

        CrusherRecipeBuilder.crushing(
                        Items.RAW_IRON,
                        ModItems.CRUSHED_IRON_ORE.get(),
                        1
                )
                .processingTime(
                        300
                )
                .save(
                        recipeOutput,
                        id(
                                "crusher/raw_iron"
                        )
                );

        CrusherRecipeBuilder.crushing(
                        ModItems.RAW_PLATINUM.get(),
                        ModItems.CRUSHED_PLATINUM_ORE.get(),
                        1
                )
                .processingTime(
                        300
                )
                .secondOutput(
                        Items.GOLD_NUGGET,
                        1,
                        0.25f
                )
                .thirdOutput(
                        Items.DIAMOND,
                        1,
                        0.10f
                )
                .save(
                        recipeOutput,
                        id(
                                "crusher/raw_platinum"
                        )
                );

        CrusherRecipeBuilder.crushing(
                        ModItems.RAW_MYTHRIL.get(),
                        ModItems.CRUSHED_MYTHRIL_ORE.get(),
                        1
                )
                .processingTime(
                        300
                )
                .save(
                        recipeOutput,
                        id(
                                "crusher/raw_mythril"
                        )
                );
    }

    private static void buildIngotAndNuggetRecipes(
            RecipeOutput recipeOutput
    ) {
        ingotAndNugget(
                recipeOutput,
                "steel",
                ModItems.STEEL_INGOT.get(),
                ModItems.STEEL_NUGGET.get()
        );

        ingotAndNugget(
                recipeOutput,
                "platinum",
                ModItems.PLATINUM_INGOT.get(),
                ModItems.PLATINUM_NUGGET.get()
        );

        ingotAndNugget(
                recipeOutput,
                "mythril",
                ModItems.MYTHRIL_INGOT.get(),
                ModItems.MYTHRIL_NUGGET.get()
        );
    }

    private static void blockAndIngot(
            RecipeOutput recipeOutput,
            String metalName,
            ItemLike block,
            ItemLike ingot
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.BUILDING_BLOCKS,
                        block
                )
                .pattern(
                        "III"
                )
                .pattern(
                        "III"
                )
                .pattern(
                        "III"
                )
                .define(
                        'I',
                        ingot
                )
                .unlockedBy(
                        "has_"
                                + metalName
                                + "_ingot",
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );

        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ingot,
                        9
                )
                .requires(
                        block
                )
                .unlockedBy(
                        "has_"
                                + metalName
                                + "_block",
                        RecipeProviderAccessor.hasItem(
                                block
                        )
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + metalName
                                + "_ingot_from_block"
                );
    }

    private static void ingotAndNugget(
            RecipeOutput recipeOutput,
            String metalName,
            ItemLike ingot,
            ItemLike nugget
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ingot
                )
                .pattern(
                        "NNN"
                )
                .pattern(
                        "NNN"
                )
                .define(
                        'N',
                        nugget
                )
                .unlockedBy(
                        "has_"
                                + metalName
                                + "_nugget",
                        RecipeProviderAccessor.hasItem(
                                nugget
                        )
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + metalName
                                + "_ingot_from_nuggets"
                );

        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        nugget,
                        6
                )
                .requires(
                        ingot
                )
                .unlockedBy(
                        "has_"
                                + metalName
                                + "_ingot",
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + metalName
                                + "_nugget_from_ingot"
                );
    }

    private static void oreBlasting(
            RecipeOutput recipeOutput,
            Item input,
            RecipeCategory category,
            Item result,
            float experience,
            int cookingTime,
            String group
    ) {
        SimpleCookingRecipeBuilder.generic(
                        Ingredient.of(
                                input
                        ),
                        category,
                        result,
                        experience,
                        cookingTime,
                        net.minecraft.world.item.crafting.RecipeSerializer.BLASTING_RECIPE,
                        BlastingRecipe::new
                )
                .group(
                        group
                )
                .unlockedBy(
                        "has_"
                                + input.builtInRegistryHolder()
                                .key()
                                .location()
                                .getPath(),
                        RecipeProviderAccessor.hasItem(
                                input
                        )
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + result.builtInRegistryHolder()
                                .key()
                                .location()
                                .getPath()
                                + "_from_blasting_"
                                + input.builtInRegistryHolder()
                                .key()
                                .location()
                                .getPath()
                );
    }

    private static ResourceLocation id(
            String path
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                KatzencraftMetalsMod.MODID,
                path
        );
    }
}
