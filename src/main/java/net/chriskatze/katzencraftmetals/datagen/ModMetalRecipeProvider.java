package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.metal.MetalDefinition;
import net.chriskatze.katzencraftmetals.metal.StandardMetalForms;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;

/**
 * Datagen-only recipes for metal material progression.
 *
 * This keeps repeated metal recipe sets out of ModRecipeProvider:
 * - metal block <-> ingot recipes
 * - ingot <-> nugget recipes
 */
public final class ModMetalRecipeProvider {

    private ModMetalRecipeProvider() {
    }

    public static void build(
            RecipeOutput recipeOutput
    ) {
        buildBlockAndIngotRecipes(
                recipeOutput
        );

        buildIngotAndNuggetRecipes(
                recipeOutput
        );
    }

    private static void buildBlockAndIngotRecipes(
            RecipeOutput recipeOutput
    ) {
        for (var metal : net.chriskatze.katzencraftmetals.metal.MetalCatalog.values()) {
            var owned = StandardMetalForms.entries().stream().filter(entry -> entry.metal().id().equals(metal.id())).toList();
            var ingot = owned.stream().filter(entry -> entry.form() == MetalDefinition.Form.INGOT).findFirst();
            var other = owned.stream().filter(entry -> entry.form() == MetalDefinition.Form.STORAGE_BLOCK).findFirst();
            if (ingot.isPresent() && other.isPresent()) blockAndIngot(recipeOutput, metal.id().getPath(), other.get().block(), ingot.get().item());
        }

    }

    private static void buildIngotAndNuggetRecipes(
            RecipeOutput recipeOutput
    ) {
        for (var metal : net.chriskatze.katzencraftmetals.metal.MetalCatalog.values()) {
            // A mod-owned nugget can reference an existing vanilla ingot (Netherite).
            // Keep both-vanilla conversions native; generate only when we own a form.
            var nugget = StandardMetalForms.entries().stream().filter(entry -> entry.metal().id().equals(metal.id()))
                    .filter(entry -> entry.form() == MetalDefinition.Form.NUGGET).findFirst();
            var ingot = metal.forms().entrySet().stream().filter(entry -> entry.getValue() == MetalDefinition.Form.INGOT)
                    .map(entry -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElseThrow()).findFirst();
            if (ingot.isPresent() && nugget.isPresent()) ingotAndNugget(recipeOutput, metal.id().getPath(), ingot.get(), nugget.get().item());
        }

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

}
