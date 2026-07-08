package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyIngredient;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FoundryAlloyRecipeBuilder implements RecipeBuilder {

    private final ResourceLocation outputMetal;
    private final int outputAmount;
    private final List<FoundryAlloyIngredient> ingredients =
            new ArrayList<>();

    private int processingTime =
            100;

    private int experience =
            1;

    private int requiredTier =
            1;

    private FoundryAlloyRecipeBuilder(
            ResourceLocation outputMetal,
            int outputAmount
    ) {
        this.outputMetal =
                outputMetal;

        this.outputAmount =
                outputAmount;
    }

    public static FoundryAlloyRecipeBuilder alloy(
            ResourceLocation outputMetal,
            int outputAmount
    ) {
        return new FoundryAlloyRecipeBuilder(
                outputMetal,
                outputAmount
        );
    }

    public FoundryAlloyRecipeBuilder ingredient(
            ResourceLocation metal,
            int amount
    ) {
        ingredients.add(
                new FoundryAlloyIngredient(
                        metal,
                        amount
                )
        );

        return this;
    }

    public FoundryAlloyRecipeBuilder processingTime(
            int ticks
    ) {
        processingTime =
                ticks;

        return this;
    }

    public FoundryAlloyRecipeBuilder experience(
            int experience
    ) {
        this.experience =
                experience;

        return this;
    }

    public FoundryAlloyRecipeBuilder requiredTier(
            int requiredTier
    ) {
        this.requiredTier =
                requiredTier;

        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(
            String name,
            Criterion<?> criterion
    ) {
        return this;
    }

    @Override
    public RecipeBuilder group(
            @Nullable String groupName
    ) {
        return this;
    }

    @Override
    public Item getResult() {
        return Items.AIR;
    }

    @Override
    public void save(
            RecipeOutput output,
            ResourceLocation id
    ) {
        output.accept(
                id,
                new FoundryAlloyRecipe(
                        List.copyOf(
                                ingredients
                        ),
                        outputMetal,
                        outputAmount,
                        processingTime,
                        experience,
                        requiredTier
                ),
                null
        );
    }
}
