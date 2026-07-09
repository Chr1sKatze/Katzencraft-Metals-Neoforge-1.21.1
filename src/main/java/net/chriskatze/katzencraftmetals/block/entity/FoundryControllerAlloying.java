package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyCatalog;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * One persistent, explicitly started alloy job owned by a Controller.
 *
 * A multi-batch job is processed strictly one batch at a time:
 *
 * 1. One batch completes its full progress cycle.
 * 2. Only that batch's ingredients are consumed.
 * 3. Only that batch's output is inserted.
 * 4. Progress resets and the next batch begins.
 *
 * Ingredient metals therefore remain in the tank until their individual batch
 * has actually finished.
 */
final class FoundryControllerAlloying {

    private final FoundryControllerBlockEntity controller;

    private final FoundryAlloyJobState job =
            new FoundryAlloyJobState();

    FoundryControllerAlloying(
            FoundryControllerBlockEntity controller
    ) {
        this.controller =
                controller;
    }

    boolean start(
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

    void tick(
            Level level,
            BlockPos pos
    ) {
        if (
                level.isClientSide()
                        || !job.hasActiveJob()
        ) {
            return;
        }

        if (!controller.ensureTankNetwork()) {
            job.statusCode =
                    FoundryControllerProcessing.STATUS_NO_TANKS;
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            job.statusCode =
                    FoundryControllerProcessing.STATUS_NO_TANKS;
            return;
        }

        network.ensureMoltenContentsMigrated();

        if (
                FoundryAlloyCatalog.byId(
                        level,
                        job.activeRecipeId
                ).isEmpty()
        ) {
            stop();
            return;
        }

        /*
         * The current batch cannot run unless its own materials and completion
         * space are still available. This prevents fuel from being wasted while
         * the batch could not possibly finish.
         */
        if (
                !FoundryAlloyAmounts.hasIngredients(
                        network,
                        job.requiredIngredients
                )
        ) {
            job.statusCode =
                    FoundryControllerProcessing.STATUS_ALLOYING;
            return;
        }

        int oneBatchInput =
                FoundryAlloyAmounts.totalAmount(
                        job.requiredIngredients
                );

        if (
                !FoundryAlloyAmounts.hasCompletionSpace(
                        network,
                        oneBatchInput,
                        job.outputAmount
                )
        ) {
            job.statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;
            return;
        }

        /*
         * Only the currently active batch advances.
         */
        if (job.progress < job.maxProgress) {
            FoundryControllerFuelSystem fuel =
                    controller.getFuelSystem();

            if (!fuel.hasAvailableFuel()) {
                job.statusCode =
                        FoundryControllerProcessing.STATUS_MISSING_FUEL;
                return;
            }

            if (!fuel.supplyBurnTick()) {
                job.statusCode =
                        FoundryControllerProcessing.STATUS_MISSING_FUEL;
                return;
            }

            job.statusCode =
                    FoundryControllerProcessing.STATUS_ALLOYING;

            job.progress++;
            controller.setChanged();

            if (job.progress < job.maxProgress) {
                return;
            }
        }

        /*
         * Exactly one batch has finished. Consume and produce exactly one batch.
         */
        if (!completeCurrentBatch(network)) {
            return;
        }

        job.completedBatches++;
        controller.addFoundryExperience(
                job.experience
        );

        if (job.completedBatches >= job.batchCount) {
            job.clear();
            controller.setChanged();
            return;
        }

        /*
         * Begin the next batch from zero on the following server tick.
         */
        job.progress = 0;

        job.statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;

        controller.setChanged();
    }

    boolean stop() {
        if (!job.hasActiveJob()) {
            return false;
        }

        /*
         * Completed batches stay completed. The unfinished current batch has not
         * consumed ingredients, so stopping needs no refund.
         */
        job.clear();
        controller.setChanged();
        return true;
    }

    void cancelAndRefund() {
        if (!job.hasActiveJob()) {
            return;
        }

        /*
         * Nothing is reserved up front anymore. Unfinished batches therefore have
         * nothing to refund.
         */
        job.clear();
        controller.setChanged();
    }

    private boolean completeCurrentBatch(
            FoundryTankNetwork network
    ) {
        Map<ResourceLocation, Integer> extracted =
                new LinkedHashMap<>();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                job.requiredIngredients.entrySet()
        ) {
            int amount =
                    network.extract(
                            entry.getKey(),
                            entry.getValue()
                    );

            if (amount != entry.getValue()) {
                if (amount > 0) {
                    extracted.merge(
                            entry.getKey(),
                            amount,
                            Integer::sum
                    );
                }

                FoundryAlloyAmounts.refund(
                        network,
                        extracted
                );

                job.statusCode =
                        FoundryControllerProcessing.STATUS_ALLOYING;

                controller.setChanged();
                return false;
            }

            extracted.put(
                    entry.getKey(),
                    amount
            );
        }

        int inserted =
                network.insert(
                        job.outputMetal,
                        job.outputAmount
                );

        if (inserted != job.outputAmount) {
            if (inserted > 0) {
                network.extract(
                        job.outputMetal,
                        inserted
                );
            }

            FoundryAlloyAmounts.refund(
                    network,
                    extracted
            );

            job.statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;

            controller.setChanged();
            return false;
        }

        return true;
    }

    boolean hasActiveJob() {
        return job.hasActiveJob();
    }

    int getProgress() {
        return job.progress;
    }

    int getMaxProgress() {
        return job.getMaxProgress();
    }

    int getStatusCode() {
        return job.statusCode;
    }

    /**
     * Returns the number of batches that have not completed yet.
     *
     * The Controller synchronizes this value to the menu, so the quantity box
     * and the ingredient/output amounts in the recipe preview count down after
     * each completed batch.
     */
    int getBatchCount() {
        return job.getRemainingBatchCount();
    }

    @Nullable
    ResourceLocation getOutputMetal() {
        return job.outputMetal;
    }

    void save(
            CompoundTag tag
    ) {
        job.save(
                tag
        );
    }

    void load(
            CompoundTag tag
    ) {
        job.load(
                tag
        );
    }
}
