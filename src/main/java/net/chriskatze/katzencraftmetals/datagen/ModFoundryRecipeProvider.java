package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.datagen.recipe.FoundryAlloyRecipeSet;
import net.chriskatze.katzencraftmetals.datagen.recipe.FoundryMeltingRecipeSet;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * Foundry-only recipe definitions.
 *
 * Keep new molten-metal melting recipes and alloy recipes here instead of
 * scattering handwritten JSON files through src/main/resources.
 */
public final class ModFoundryRecipeProvider {

    private ModFoundryRecipeProvider() {
    }

    public static void build(
            RecipeOutput recipeOutput
    ) {
        buildIronMeltingRecipes(
                recipeOutput
        );

        buildCopperMeltingRecipes(
                recipeOutput
        );

        buildGoldMeltingRecipes(
                recipeOutput
        );

        buildSteelMeltingRecipes(
                recipeOutput
        );

        buildPlatinumMeltingRecipes(
                recipeOutput
        );

        buildMythrilMeltingRecipes(
                recipeOutput
        );

        buildAlloyRecipes(
                recipeOutput
        );
    }

    private static void buildIronMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.IRON
                )
                .raw(
                        Items.RAW_IRON,
                        "raw_iron"
                )
                .ingot(
                        Items.IRON_INGOT,
                        "iron_ingot"
                )
                .nugget(
                        Items.IRON_NUGGET,
                        "iron_nugget"
                )
                .rawBlock(
                        Items.RAW_IRON_BLOCK,
                        "raw_iron_block"
                )
                .block(
                        Items.IRON_BLOCK,
                        "iron_block"
                )
                .ore(
                        Items.IRON_ORE,
                        "iron_ore"
                )
                .ore(
                        Items.DEEPSLATE_IRON_ORE,
                        "deepslate_iron_ore"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildCopperMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.COPPER
                )
                .raw(
                        Items.RAW_COPPER,
                        "raw_copper"
                )
                .ingot(
                        Items.COPPER_INGOT,
                        "copper_ingot"
                )
                .rawBlock(
                        Items.RAW_COPPER_BLOCK,
                        "raw_copper_block"
                )
                .block(
                        Items.COPPER_BLOCK,
                        "copper_block"
                )
                .ore(
                        Items.COPPER_ORE,
                        "copper_ore"
                )
                .ore(
                        Items.DEEPSLATE_COPPER_ORE,
                        "deepslate_copper_ore"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildGoldMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.GOLD
                )
                .raw(
                        Items.RAW_GOLD,
                        "raw_gold"
                )
                .ingot(
                        Items.GOLD_INGOT,
                        "gold_ingot"
                )
                .nugget(
                        Items.GOLD_NUGGET,
                        "gold_nugget"
                )
                .rawBlock(
                        Items.RAW_GOLD_BLOCK,
                        "raw_gold_block"
                )
                .block(
                        Items.GOLD_BLOCK,
                        "gold_block"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildSteelMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.STEEL
                )
                .ingot(
                        ModItems.STEEL_INGOT.get(),
                        "steel_ingot"
                )
                .nugget(
                        ModItems.STEEL_NUGGET.get(),
                        "steel_nugget"
                )
                .block(
                        ModBlocks.STEEL_BLOCK.get()
                                .asItem(),
                        "steel_block"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildPlatinumMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.PLATINUM
                )
                .raw(
                        ModItems.RAW_PLATINUM.get(),
                        "raw_platinum"
                )
                .ingot(
                        ModItems.PLATINUM_INGOT.get(),
                        "platinum_ingot"
                )
                .nugget(
                        ModItems.PLATINUM_NUGGET.get(),
                        "platinum_nugget"
                )
                .block(
                        ModBlocks.PLATINUM_BLOCK.get()
                                .asItem(),
                        "platinum_block"
                )
                .ore(
                        ModBlocks.PLATINUM_ORE.get()
                                .asItem(),
                        "platinum_ore"
                )
                .ore(
                        ModBlocks.DEEPSLATE_PLATINUM_ORE.get()
                                .asItem(),
                        "deepslate_platinum_ore"
                )
                .ore(
                        ModBlocks.NETHER_PLATINUM_ORE.get()
                                .asItem(),
                        "nether_platinum_ore"
                )
                .ore(
                        ModBlocks.END_PLATINUM_ORE.get()
                                .asItem(),
                        "end_platinum_ore"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildMythrilMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        FoundryMeltingRecipeSet.forMetal(
                        ModMoltenMetals.MYTHRIL
                )
                .raw(
                        ModItems.RAW_MYTHRIL.get(),
                        "raw_mythril"
                )
                .ingot(
                        ModItems.MYTHRIL_INGOT.get(),
                        "mythril_ingot"
                )
                .nugget(
                        ModItems.MYTHRIL_NUGGET.get(),
                        "mythril_nugget"
                )
                .block(
                        ModBlocks.MYTHRIL_BLOCK.get()
                                .asItem(),
                        "mythril_block"
                )
                .ore(
                        ModBlocks.MYTHRIL_ORE.get()
                                .asItem(),
                        "mythril_ore"
                )
                .ore(
                        ModBlocks.DEEPSLATE_MYTHRIL_ORE.get()
                                .asItem(),
                        "deepslate_mythril_ore"
                )
                .ore(
                        ModBlocks.NETHER_MYTHRIL_ORE.get()
                                .asItem(),
                        "nether_mythril_ore"
                )
                .ore(
                        ModBlocks.END_MYTHRIL_ORE.get()
                                .asItem(),
                        "end_mythril_ore"
                )
                .save(
                        recipeOutput
                );
    }

    private static void buildAlloyRecipes(
            RecipeOutput recipeOutput
    ) {
        /*
         * Temporary Foundry progression:
         *
         * Steel is currently a refinement-style alloy recipe because the alloy
         * system only accepts molten metal ingredients. We can add carbon/coal
         * support later if we want proper iron + carbon steel.
         */
        FoundryAlloyRecipeSet.forMetal(
                        ModMoltenMetals.STEEL
                )
                .ingredient(
                        ModMoltenMetals.IRON,
                        6
                )
                .experience(
                        1
                )
                .save(
                        recipeOutput
                );

        FoundryAlloyRecipeSet.forMetal(
                        ModMoltenMetals.PLATINUM
                )
                .ingredient(
                        ModMoltenMetals.IRON,
                        3
                )
                .ingredient(
                        ModMoltenMetals.GOLD,
                        3
                )
                .experience(
                        2
                )
                .save(
                        recipeOutput
                );

        FoundryAlloyRecipeSet.forMetal(
                        ModMoltenMetals.MYTHRIL
                )
                .ingredient(
                        ModMoltenMetals.STEEL,
                        3
                )
                .ingredient(
                        ModMoltenMetals.PLATINUM,
                        3
                )
                .processingTime(
                        140
                )
                .experience(
                        3
                )
                .requiredTier(
                        2
                )
                .save(
                        recipeOutput
                );
    }
}
