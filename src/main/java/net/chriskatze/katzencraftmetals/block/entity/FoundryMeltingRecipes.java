package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Small helper for Foundry melting recipe lookup and recipe-result comparison.
 *
 * FoundryControllerProcessing owns the melting state machine.
 * This class only answers recipe questions.
 */
public final class FoundryMeltingRecipes {

    private FoundryMeltingRecipes() {
    }

    static boolean canMelt(
            @Nullable Level level,
            ItemStack stack
    ) {
        return find(
                level,
                stack
        ).isPresent();
    }

    public static Optional<FoundryMeltingRecipe> find(
            @Nullable Level level,
            ItemStack stack
    ) {
        if (
                level == null
                        || stack.isEmpty()
        ) {
            return Optional.empty();
        }

        // Explicit recycling metadata wins over ordinary recipes for the same stack.
        return resolve(level.getRecipeManager().getRecipesFor(ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                new SingleRecipeInput(stack), level).stream().map(holder -> holder.value()).toList(), stack);
    }

    public static Optional<FoundryMeltingRecipe> resolve(java.util.List<FoundryMeltingRecipe> recipes, ItemStack stack) {
        return recipes.stream().filter(recipe -> recipe.ingredient().test(stack))
                .filter(recipe -> ModMoltenMetals.contains(recipe.moltenMetal()))
                .min(java.util.Comparator.comparing(recipe -> recipe.recycling().isEmpty()))
                .flatMap(recipe -> recipe.resolve(stack));
    }

    static boolean sameResult(
            FoundryMeltingRecipe first,
            FoundryMeltingRecipe second
    ) {
        return first.moltenAmount()
                == second.moltenAmount()
                && first.processingTime()
                == second.processingTime()
                && first.moltenMetal()
                .equals(
                        second.moltenMetal()
                );
    }
}
