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
final class FoundryMeltingRecipes {

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

    static Optional<FoundryMeltingRecipe> find(
            @Nullable Level level,
            ItemStack stack
    ) {
        if (
                level == null
                        || stack.isEmpty()
        ) {
            return Optional.empty();
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                        new SingleRecipeInput(
                                stack
                        ),
                        level
                )
                .map(
                        holder ->
                                holder.value()
                )
                .filter(
                        recipe ->
                                ModMoltenMetals.contains(
                                        recipe.moltenMetal()
                                )
                );
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
