package net.chriskatze.katzencraftmetals.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.chriskatze.katzencraftmetals.metal.MetalAlloy;
import net.chriskatze.katzencraftmetals.metal.MetalDefinition;
import net.chriskatze.katzencraftmetals.metal.MetalCatalog;
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
        net.chriskatze.katzencraftmetals.metal.MetalRecyclingRecipes.generate(recipeOutput);
        buildCatalogStandardMelting(recipeOutput);
        buildIronMeltingRecipes(
                recipeOutput
        );

        buildCopperMeltingRecipes(
                recipeOutput
        );

        buildGoldMeltingRecipes(
                recipeOutput
        );

        buildPlatinumMeltingRecipes(
                recipeOutput
        );

        buildMythrilMeltingRecipes(
                recipeOutput
        );

        FoundryMeltingRecipeSet.forMetal(ModMoltenMetals.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("netherite")).orElseThrow())
                .raw(Items.NETHERITE_SCRAP, "raw_netherite")
                .ore(Items.ANCIENT_DEBRIS, "netherite_ore")
                .save(recipeOutput);

        buildAlloyRecipes(
                recipeOutput
        );
    }

    private static void buildCatalogStandardMelting(RecipeOutput output) {
        for (var definition : MetalCatalog.values()) {
            if (definition.foundry() == null) continue;
            var molten = ModMoltenMetals.get(definition.id()).orElseThrow();
            var recipes = FoundryMeltingRecipeSet.forMetal(molten);
            definition.forms().forEach((id, form) -> {
                if (form != MetalDefinition.Form.INGOT
                        && form != MetalDefinition.Form.NUGGET
                        && form != MetalDefinition.Form.STORAGE_BLOCK) return;
                var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow(
                        () -> new IllegalStateException("Missing Foundry input form: " + id));
                switch (form) {
                    case INGOT -> recipes.ingot(item, id.getPath());
                    case NUGGET -> recipes.nugget(item, id.getPath());
                    case STORAGE_BLOCK -> recipes.block(item, id.getPath());
                    default -> {}
                }
            });
            recipes.save(output);
        }
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
                .rawBlock(
                        Items.RAW_IRON_BLOCK,
                        "raw_iron_block"
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
                .rawBlock(
                        Items.RAW_COPPER_BLOCK,
                        "raw_copper_block"
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
                .rawBlock(
                        Items.RAW_GOLD_BLOCK,
                        "raw_gold_block"
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
        for (var definition : MetalCatalog.values()) {
            if (definition.alloy() == null) continue;
            var recipes = FoundryAlloyRecipeSet.forMetal(ModMoltenMetals.get(definition.id()).orElseThrow())
                    .outputAmount(MetalAlloy.UNITS_PER_INGOT)
                    .processingTime(definition.alloy().processingTime());
            for (var input : definition.alloy().inputs())
                recipes.ingredient(ModMoltenMetals.get(input.metal()).orElseThrow(), input.moltenUnits());
            recipes.save(recipeOutput);
        }
    }
}
