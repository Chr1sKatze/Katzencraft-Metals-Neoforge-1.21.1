package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyCatalog;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Map;
import java.util.Optional;

/**
 * Validation and setup for explicitly starting an alloy job.
 *
 * FoundryControllerAlloying owns the running job behavior. This helper keeps
 * the one-time start checks and state setup out of the tick/state-machine class.
 */
final class FoundryAlloyStarter {

    private FoundryAlloyStarter() {
    }

    static boolean start(
            FoundryControllerBlockEntity controller,
            FoundryAlloyJobState job,
            int recipeIndex,
            int requestedBatches
    ) {
        if (
                job.hasActiveJob()
                        || controller.getLevel() == null
                        || requestedBatches < 1
                        || requestedBatches > 99
        ) {
            return false;
        }

        Optional<RecipeHolder<FoundryAlloyRecipe>> holderOptional =
                FoundryAlloyCatalog.byIndex(
                        controller.getLevel(),
                        recipeIndex
                );

        if (holderOptional.isEmpty()) {
            return false;
        }

        RecipeHolder<FoundryAlloyRecipe> holder =
                holderOptional.get();

        FoundryAlloyRecipe recipe =
                holder.value();

        if (
                recipe.requiredTier()
                        > controller.getFoundryTier()
                        || !controller.isAlloyRecipeUnlocked(
                        recipe
                )
                        || !ModMoltenMetals.contains(
                        recipe.outputMetal()
                )
        ) {
            return false;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (
                network == null
                        && !controller.ensureTankNetwork()
        ) {
            return false;
        }

        network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            return false;
        }

        network.ensureMoltenContentsMigrated();

        Map<ResourceLocation, Integer> oneBatchRequirements =
                FoundryAlloyAmounts.calculateRequirements(
                        recipe,
                        1
                );

        Map<ResourceLocation, Integer> completeJobRequirements =
                FoundryAlloyAmounts.calculateRequirements(
                        recipe,
                        requestedBatches
                );

        if (
                oneBatchRequirements.isEmpty()
                        || completeJobRequirements.isEmpty()
                        || !FoundryAlloyAmounts.hasIngredients(
                        network,
                        completeJobRequirements
                )
        ) {
            return false;
        }

        int completeJobInput =
                FoundryAlloyAmounts.totalAmount(
                        completeJobRequirements
                );

        int completeJobOutput =
                FoundryAlloyAmounts.safeMultiply(
                        recipe.outputAmount(),
                        requestedBatches
                );

        if (
                completeJobInput <= 0
                        || completeJobOutput <= 0
                        || recipe.outputAmount() <= 0
                        || recipe.processingTime() <= 0
                        || recipe.experience() < 0
        ) {
            return false;
        }

        /*
         * Starting multiple batches still requires enough total material and
         * enough final tank capacity for the whole requested job, but nothing is
         * consumed until each individual batch completes.
         */
        if (
                !FoundryAlloyAmounts.hasCompletionSpace(
                        network,
                        completeJobInput,
                        completeJobOutput
                )
        ) {
            return false;
        }

        job.start(
                holder.id(),
                recipe.outputMetal(),
                oneBatchRequirements,
                recipe.outputAmount(),
                requestedBatches,
                recipe.processingTime(),
                recipe.experience()
        );

        controller.setChanged();
        return true;
    }
}
