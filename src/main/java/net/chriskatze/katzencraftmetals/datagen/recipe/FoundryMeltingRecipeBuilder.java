package net.chriskatze.katzencraftmetals.datagen.recipe;

import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class FoundryMeltingRecipeBuilder
        implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ResourceLocation moltenMetal;
    private final int moltenAmount;

    private int processingTime =
            20;

    private FoundryMeltingRecipeBuilder(
            Ingredient ingredient,
            ResourceLocation moltenMetal,
            int moltenAmount
    ) {
        if (moltenAmount <= 0) {
            throw new IllegalArgumentException(
                    "Foundry molten amount must be positive."
            );
        }

        this.ingredient =
                ingredient;

        this.moltenMetal =
                moltenMetal;

        this.moltenAmount =
                moltenAmount;
    }

    public static FoundryMeltingRecipeBuilder melting(
            Item input,
            ResourceLocation moltenMetal,
            int moltenAmount
    ) {
        return new FoundryMeltingRecipeBuilder(
                Ingredient.of(input),
                moltenMetal,
                moltenAmount
        );
    }

    public static FoundryMeltingRecipeBuilder melting(
            Ingredient input,
            ResourceLocation moltenMetal,
            int moltenAmount
    ) {
        return new FoundryMeltingRecipeBuilder(
                input,
                moltenMetal,
                moltenAmount
        );
    }

    public FoundryMeltingRecipeBuilder processingTime(
            int ticks
    ) {
        if (ticks <= 0) {
            throw new IllegalArgumentException(
                    "Foundry processing time must be positive."
            );
        }

        processingTime =
                ticks;

        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(
            String name,
            Criterion<?> criterion
    ) {
        /*
         * Foundry recipes are machine recipes and are not displayed in the
         * vanilla recipe book, so they do not need advancement criteria.
         */
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
        /*
         * The recipe creates molten storage, not an ItemStack.
         */
        return Items.AIR;
    }

    @Override
    public void save(
            RecipeOutput output,
            ResourceLocation id
    ) {
        output.accept(
                id,
                new FoundryMeltingRecipe(
                        ingredient,
                        moltenMetal,
                        moltenAmount,
                        processingTime
                ),
                null
        );
    }
}
