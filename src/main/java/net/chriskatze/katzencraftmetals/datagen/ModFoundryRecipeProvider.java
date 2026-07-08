package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.datagen.recipe.FoundryMeltingRecipeBuilder;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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

        buildAlloyRecipes(
                recipeOutput
        );
    }

    private static void buildIronMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        foundryMelting(
                recipeOutput,
                Items.RAW_IRON,
                ModMoltenMetals.IRON.id(),
                6,
                20,
                "raw_iron"
        );

        foundryMelting(
                recipeOutput,
                Items.IRON_INGOT,
                ModMoltenMetals.IRON.id(),
                6,
                20,
                "iron_ingot"
        );

        foundryMelting(
                recipeOutput,
                Items.IRON_NUGGET,
                ModMoltenMetals.IRON.id(),
                1,
                10,
                "iron_nugget"
        );

        foundryMelting(
                recipeOutput,
                Items.RAW_IRON_BLOCK,
                ModMoltenMetals.IRON.id(),
                54,
                120,
                "raw_iron_block"
        );

        foundryMelting(
                recipeOutput,
                Items.IRON_BLOCK,
                ModMoltenMetals.IRON.id(),
                54,
                120,
                "iron_block"
        );

        foundryMelting(
                recipeOutput,
                Items.IRON_ORE,
                ModMoltenMetals.IRON.id(),
                6,
                20,
                "iron_ore"
        );

        foundryMelting(
                recipeOutput,
                Items.DEEPSLATE_IRON_ORE,
                ModMoltenMetals.IRON.id(),
                6,
                20,
                "deepslate_iron_ore"
        );
    }

    private static void buildCopperMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        foundryMelting(
                recipeOutput,
                Items.RAW_COPPER,
                ModMoltenMetals.COPPER.id(),
                6,
                20,
                "raw_copper"
        );

        foundryMelting(
                recipeOutput,
                Items.COPPER_INGOT,
                ModMoltenMetals.COPPER.id(),
                6,
                20,
                "copper_ingot"
        );

        foundryMelting(
                recipeOutput,
                Items.RAW_COPPER_BLOCK,
                ModMoltenMetals.COPPER.id(),
                54,
                120,
                "raw_copper_block"
        );

        foundryMelting(
                recipeOutput,
                Items.COPPER_BLOCK,
                ModMoltenMetals.COPPER.id(),
                54,
                120,
                "copper_block"
        );

        foundryMelting(
                recipeOutput,
                Items.COPPER_ORE,
                ModMoltenMetals.COPPER.id(),
                6,
                20,
                "copper_ore"
        );

        foundryMelting(
                recipeOutput,
                Items.DEEPSLATE_COPPER_ORE,
                ModMoltenMetals.COPPER.id(),
                6,
                20,
                "deepslate_copper_ore"
        );
    }

    private static void buildGoldMeltingRecipes(
            RecipeOutput recipeOutput
    ) {
        foundryMelting(
                recipeOutput,
                Items.RAW_GOLD,
                ModMoltenMetals.GOLD.id(),
                6,
                20,
                "raw_gold"
        );

        foundryMelting(
                recipeOutput,
                Items.GOLD_INGOT,
                ModMoltenMetals.GOLD.id(),
                6,
                20,
                "gold_ingot"
        );

        foundryMelting(
                recipeOutput,
                Items.GOLD_NUGGET,
                ModMoltenMetals.GOLD.id(),
                1,
                10,
                "gold_nugget"
        );

        foundryMelting(
                recipeOutput,
                Items.RAW_GOLD_BLOCK,
                ModMoltenMetals.GOLD.id(),
                54,
                120,
                "raw_gold_block"
        );

        foundryMelting(
                recipeOutput,
                Items.GOLD_BLOCK,
                ModMoltenMetals.GOLD.id(),
                54,
                120,
                "gold_block"
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
        FoundryAlloyRecipeBuilder.alloy(
                        ModMoltenMetals.STEEL.id(),
                        6
                )
                .ingredient(
                        ModMoltenMetals.IRON.id(),
                        6
                )
                .processingTime(
                        100
                )
                .experience(
                        1
                )
                .requiredTier(
                        1
                )
                .save(
                        recipeOutput,
                        foundryAlloyId(
                                "steel"
                        )
                );

        FoundryAlloyRecipeBuilder.alloy(
                        ModMoltenMetals.PLATINUM.id(),
                        6
                )
                .ingredient(
                        ModMoltenMetals.IRON.id(),
                        3
                )
                .ingredient(
                        ModMoltenMetals.GOLD.id(),
                        3
                )
                .processingTime(
                        100
                )
                .experience(
                        2
                )
                .requiredTier(
                        1
                )
                .save(
                        recipeOutput,
                        foundryAlloyId(
                                "platinum"
                        )
                );

        FoundryAlloyRecipeBuilder.alloy(
                        ModMoltenMetals.MYTHRIL.id(),
                        6
                )
                .ingredient(
                        ModMoltenMetals.STEEL.id(),
                        3
                )
                .ingredient(
                        ModMoltenMetals.PLATINUM.id(),
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
                        recipeOutput,
                        foundryAlloyId(
                                "mythril"
                        )
                );
    }

    private static void foundryMelting(
            RecipeOutput recipeOutput,
            Item input,
            ResourceLocation moltenMetal,
            int moltenAmount,
            int processingTime,
            String recipeName
    ) {
        FoundryMeltingRecipeBuilder.melting(
                        input,
                        moltenMetal,
                        moltenAmount
                )
                .processingTime(
                        processingTime
                )
                .save(
                        recipeOutput,
                        foundryMeltingId(
                                recipeName
                        )
                );
    }

    private static ResourceLocation foundryMeltingId(
            String recipeName
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                KatzencraftMetalsMod.MODID,
                "foundry_melting/"
                        + recipeName
        );
    }

    private static ResourceLocation foundryAlloyId(
            String recipeName
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                KatzencraftMetalsMod.MODID,
                "foundry_alloy/"
                        + recipeName
        );
    }
}
