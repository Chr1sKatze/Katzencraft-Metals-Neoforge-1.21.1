package net.chriskatze.katzencraftmetals.datagen.recipe;

import net.chriskatze.katzencraftmetals.recipe.CrusherRecipe;
import net.minecraft.advancements.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class CrusherRecipeBuilder implements RecipeBuilder {

    private final Ingredient ingredient;
    private final ItemStack mainOutput;

    private ItemStack secondOutput = ItemStack.EMPTY;
    private float secondChance = 0.0f;

    private ItemStack thirdOutput = ItemStack.EMPTY;
    private float thirdChance = 0.0f;

    private int processingTime = 100;

    public CrusherRecipeBuilder(Ingredient ingredient, ItemStack mainOutput) {
        this.ingredient = ingredient;
        this.mainOutput = mainOutput;
    }

    public static CrusherRecipeBuilder crushing(Item input, Item output, int count) {
        return new CrusherRecipeBuilder(
                Ingredient.of(input),
                new ItemStack(output, count)
        );
    }

    public CrusherRecipeBuilder secondOutput(Item item, int count, float chance) {
        this.secondOutput = new ItemStack(item, count);
        this.secondChance = chance;
        return this;
    }

    public CrusherRecipeBuilder thirdOutput(Item item, int count, float chance) {
        this.thirdOutput = new ItemStack(item, count);
        this.thirdChance = chance;
        return this;
    }

    public CrusherRecipeBuilder processingTime(int ticks) {
        this.processingTime = ticks;
        return this;
    }

    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return mainOutput.getItem();
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        CrusherRecipe recipe = new CrusherRecipe(
                ingredient,
                mainOutput,
                secondOutput,
                secondChance,
                thirdOutput,
                thirdChance,
                processingTime
        );

        output.accept(id, recipe, null);
    }
}