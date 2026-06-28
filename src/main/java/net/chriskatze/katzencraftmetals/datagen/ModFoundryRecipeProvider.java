package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.datagen.recipe.FoundryMeltingRecipeBuilder;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * Kept separate from ModRecipeProvider so the Foundry recipe system can
 * grow without making the already-large general provider harder to manage.
 *
 * Its inherited RecipeProvider name is handled externally by
 * NamedDataProvider because RecipeProvider#getName() is final.
 */
public class ModFoundryRecipeProvider
        extends RecipeProvider {

    public ModFoundryRecipeProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        super(
                output,
                lookupProvider
        );
    }

    @Override
    protected void buildRecipes(
            RecipeOutput recipeOutput
    ) {
        // Raw Iron -> 6 molten Iron
        FoundryMeltingRecipeBuilder.melting(
                        Items.RAW_IRON,
                        ModMoltenMetals.IRON.id(),
                        ModMoltenMetals.IRON.unitsPerOre()
                )
                .processingTime(20)
                .save(
                        recipeOutput,
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "foundry_melting/raw_iron"
                        )
                );

        // Raw Copper -> 6 molten Copper
        FoundryMeltingRecipeBuilder.melting(
                        Items.RAW_COPPER,
                        ModMoltenMetals.COPPER.id(),
                        ModMoltenMetals.COPPER.unitsPerOre()
                )
                .processingTime(20)
                .save(
                        recipeOutput,
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "foundry_melting/raw_copper"
                        )
                );
    }
}
