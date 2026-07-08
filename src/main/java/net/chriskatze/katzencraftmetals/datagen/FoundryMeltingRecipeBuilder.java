package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public final class FoundryMeltingRecipeBuilder implements RecipeBuilder {

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
        this.ingredient =
                ingredient;

        this.moltenMetal =
                moltenMetal;

        this.moltenAmount =
                moltenAmount;
    }

    public static FoundryMeltingRecipeBuilder melting(
            ItemLike input,
            ResourceLocation moltenMetal,
            int moltenAmount
    ) {
        return new FoundryMeltingRecipeBuilder(
                Ingredient.of(
                        input
                ),
                moltenMetal,
                moltenAmount
        );
    }

    public FoundryMeltingRecipeBuilder processingTime(
            int ticks
    ) {
        processingTime =
                ticks;

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
