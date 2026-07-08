package net.chriskatze.katzencraftmetals.datagen.recipe;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Small datagen helper for standard Foundry melting recipe groups.
 *
 * This is only for datagen. Runtime molten-metal registration still belongs
 * in ModMoltenMetals.
 */
public final class FoundryMeltingRecipeSet {

    private final MoltenMetalDefinition metal;
    private final List<Entry> entries =
            new ArrayList<>();

    private FoundryMeltingRecipeSet(
            MoltenMetalDefinition metal
    ) {
        this.metal =
                metal;
    }

    public static FoundryMeltingRecipeSet forMetal(
            MoltenMetalDefinition metal
    ) {
        return new FoundryMeltingRecipeSet(
                metal
        );
    }

    public FoundryMeltingRecipeSet raw(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                20,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet ingot(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                20,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet nugget(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                1,
                10,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet rawBlock(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                metal.unitsPerOre()
                        * 9,
                120,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet block(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                metal.unitsPerOre()
                        * 9,
                120,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet ore(
            Item input,
            String recipeName
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                20,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet item(
            Item input,
            int moltenAmount,
            int processingTime,
            String recipeName
    ) {
        entries.add(
                new Entry(
                        input,
                        moltenAmount,
                        processingTime,
                        recipeName
                )
        );

        return this;
    }

    public void save(
            RecipeOutput recipeOutput
    ) {
        for (Entry entry : entries) {
            FoundryMeltingRecipeBuilder.melting(
                            entry.input(),
                            metal.id(),
                            entry.moltenAmount()
                    )
                    .processingTime(
                            entry.processingTime()
                    )
                    .save(
                            recipeOutput,
                            foundryMeltingId(
                                    entry.recipeName()
                            )
                    );
        }
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

    private record Entry(
            Item input,
            int moltenAmount,
            int processingTime,
            String recipeName
    ) {
    }
}
