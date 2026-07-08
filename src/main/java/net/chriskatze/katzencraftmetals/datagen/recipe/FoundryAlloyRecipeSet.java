package net.chriskatze.katzencraftmetals.datagen.recipe;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.datagen.FoundryAlloyRecipeBuilder;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Small datagen helper for Foundry alloy recipes.
 *
 * This is only for datagen. Runtime molten-metal registration still belongs
 * in ModMoltenMetals.
 */
public final class FoundryAlloyRecipeSet {

    private final MoltenMetalDefinition outputMetal;
    private final List<IngredientEntry> ingredients =
            new ArrayList<>();

    private int outputAmount;
    private int processingTime =
            100;
    private int experience =
            1;
    private int requiredTier =
            1;
    private String recipeName;

    private FoundryAlloyRecipeSet(
            MoltenMetalDefinition outputMetal
    ) {
        this.outputMetal =
                outputMetal;

        this.outputAmount =
                outputMetal.unitsPerOre();

        this.recipeName =
                outputMetal.id()
                        .getPath();
    }

    public static FoundryAlloyRecipeSet forMetal(
            MoltenMetalDefinition outputMetal
    ) {
        return new FoundryAlloyRecipeSet(
                outputMetal
        );
    }

    public FoundryAlloyRecipeSet ingredient(
            MoltenMetalDefinition metal,
            int amount
    ) {
        ingredients.add(
                new IngredientEntry(
                        metal,
                        amount
                )
        );

        return this;
    }

    public FoundryAlloyRecipeSet outputAmount(
            int amount
    ) {
        this.outputAmount =
                amount;

        return this;
    }

    public FoundryAlloyRecipeSet processingTime(
            int ticks
    ) {
        this.processingTime =
                ticks;

        return this;
    }

    public FoundryAlloyRecipeSet experience(
            int experience
    ) {
        this.experience =
                experience;

        return this;
    }

    public FoundryAlloyRecipeSet requiredTier(
            int tier
    ) {
        this.requiredTier =
                tier;

        return this;
    }

    public FoundryAlloyRecipeSet recipeName(
            String recipeName
    ) {
        this.recipeName =
                recipeName;

        return this;
    }

    public void save(
            RecipeOutput recipeOutput
    ) {
        if (ingredients.isEmpty()) {
            throw new IllegalStateException(
                    "Foundry alloy recipe must have at least one ingredient: "
                            + outputMetal.id()
            );
        }

        FoundryAlloyRecipeBuilder builder =
                FoundryAlloyRecipeBuilder.alloy(
                        outputMetal.id(),
                        outputAmount
                )
                        .processingTime(
                                processingTime
                        )
                        .experience(
                                experience
                        )
                        .requiredTier(
                                requiredTier
                        );

        for (IngredientEntry ingredient : ingredients) {
            builder.ingredient(
                    ingredient.metal()
                            .id(),
                    ingredient.amount()
            );
        }

        builder.save(
                recipeOutput,
                foundryAlloyId(
                        recipeName
                )
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

    private record IngredientEntry(
            MoltenMetalDefinition metal,
            int amount
    ) {
    }
}
