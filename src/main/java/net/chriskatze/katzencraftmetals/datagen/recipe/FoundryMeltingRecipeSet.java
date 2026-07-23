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

    /*
     * Iron is the balance baseline:
     *
     * 1 Coal = 1600 burn ticks
     * 1 Iron melting recipe = 100 processing ticks
     * 1600 / 100 = 16 Iron per Coal
     */
    public static final int BASE_ITEM_PROCESSING_TIME = 100;
    public static final int BASE_NUGGET_PROCESSING_TIME = 12;
    public static final int BASE_BLOCK_PROCESSING_TIME = 900;

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
        return raw(
                input,
                recipeName,
                BASE_ITEM_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet raw(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                processingTime,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet ingot(
            Item input,
            String recipeName
    ) {
        return ingot(
                input,
                recipeName,
                BASE_ITEM_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet ingot(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                processingTime,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet nugget(
            Item input,
            String recipeName
    ) {
        return nugget(
                input,
                recipeName,
                BASE_NUGGET_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet nugget(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                1,
                processingTime,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet rawBlock(
            Item input,
            String recipeName
    ) {
        return rawBlock(
                input,
                recipeName,
                BASE_BLOCK_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet rawBlock(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                metal.unitsPerOre()
                        * 9,
                processingTime,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet block(
            Item input,
            String recipeName
    ) {
        return block(
                input,
                recipeName,
                BASE_BLOCK_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet block(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                metal.unitsPerOre()
                        * 9,
                processingTime,
                recipeName
        );
    }

    public FoundryMeltingRecipeSet ore(
            Item input,
            String recipeName
    ) {
        return ore(
                input,
                recipeName,
                BASE_ITEM_PROCESSING_TIME
        );
    }

    public FoundryMeltingRecipeSet ore(
            Item input,
            String recipeName,
            int processingTime
    ) {
        return item(
                input,
                metal.unitsPerOre(),
                processingTime,
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
                        Math.max(
                                1,
                                processingTime
                        ),
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
