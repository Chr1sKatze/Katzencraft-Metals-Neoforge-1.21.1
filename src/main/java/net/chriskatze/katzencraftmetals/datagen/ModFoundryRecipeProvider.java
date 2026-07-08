package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.datagen.recipe.FoundryMeltingRecipeBuilder;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

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
        buildGoldMeltingRecipes(
                recipeOutput
        );

        buildAlloyRecipes(
                recipeOutput
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
