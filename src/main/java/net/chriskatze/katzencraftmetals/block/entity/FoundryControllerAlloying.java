package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyCatalog;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyIngredient;
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

    private static final String REQUIRED_INGREDIENTS_TAG =
            "RequiredAlloyIngredients";

    private final FoundryControllerBlockEntity controller;

    @Nullable
    private ResourceLocation activeRecipeId;

    @Nullable
    private ResourceLocation outputMetal;

    /**
     * Ingredients required for exactly one batch.
     */
    private final Map<ResourceLocation, Integer> requiredIngredients =
            new LinkedHashMap<>();

    /**
     * Output produced by exactly one completed batch.
     */
    private int outputAmount;

    /**
     * Total number of batches requested when the job started.
     */
    private int batchCount;

    /**
     * Number of batches that have already completed and produced output.
     */
    private int completedBatches;

    /**
     * Progress of the currently running batch only.
     */
    private int progress;

    /**
     * Required progress for one batch only.
     */
    private int maxProgress = 1;

    /**
     * Experience granted by one completed batch.
     */
    private int experience;

    private int statusCode =
            FoundryControllerProcessing.STATUS_READY;

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
                hasActiveJob()
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
                calculateRequirements(
                        recipe,
                        1
                );

        Map<ResourceLocation, Integer> completeJobRequirements =
                calculateRequirements(
                        recipe,
                        requestedBatches
                );

        if (
                oneBatchRequirements.isEmpty()
                        || completeJobRequirements.isEmpty()
                        || !hasIngredients(
                        network,
                        completeJobRequirements
                )
        ) {
            return false;
        }

        int completeJobInput =
                totalAmount(
                        completeJobRequirements
                );

        int completeJobOutput =
                safeMultiply(
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
                !hasCompletionSpace(
                        network,
                        completeJobInput,
                        completeJobOutput
                )
        ) {
            return false;
        }

        activeRecipeId =
                holder.id();

        outputMetal =
                recipe.outputMetal();

        requiredIngredients.clear();
        requiredIngredients.putAll(
                oneBatchRequirements
        );

        outputAmount =
                recipe.outputAmount();

        batchCount =
                requestedBatches;

        completedBatches = 0;
        progress = 0;

        maxProgress =
                recipe.processingTime();

        experience =
                recipe.experience();

        statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;

        controller.setChanged();
        return true;
    }

    void tick(
            Level level,
            BlockPos pos
    ) {
        if (
                level.isClientSide()
                        || !hasActiveJob()
        ) {
            return;
        }

        if (!controller.ensureTankNetwork()) {
            statusCode =
                    FoundryControllerProcessing.STATUS_NO_TANKS;
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            statusCode =
                    FoundryControllerProcessing.STATUS_NO_TANKS;
            return;
        }

        network.ensureMoltenContentsMigrated();

        if (
                FoundryAlloyCatalog.byId(
                        level,
                        activeRecipeId
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
                !hasIngredients(
                        network,
                        requiredIngredients
                )
        ) {
            statusCode =
                    FoundryControllerProcessing.STATUS_ALLOYING;
            return;
        }

        int oneBatchInput =
                totalAmount(
                        requiredIngredients
                );

        if (
                !hasCompletionSpace(
                        network,
                        oneBatchInput,
                        outputAmount
                )
        ) {
            statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;
            return;
        }

        /*
         * Only the currently active batch advances.
         */
        if (progress < maxProgress) {
            FoundryControllerFuelSystem fuel =
                    controller.getFuelSystem();

            if (!fuel.hasAvailableFuel()) {
                statusCode =
                        FoundryControllerProcessing.STATUS_MISSING_FUEL;
                return;
            }

            if (!fuel.supplyBurnTick()) {
                statusCode =
                        FoundryControllerProcessing.STATUS_MISSING_FUEL;
                return;
            }

            statusCode =
                    FoundryControllerProcessing.STATUS_ALLOYING;

            progress++;
            controller.setChanged();

            if (progress < maxProgress) {
                return;
            }
        }

        /*
         * Exactly one batch has finished. Consume and produce exactly one batch.
         */
        if (!completeCurrentBatch(network)) {
            return;
        }

        completedBatches++;
        controller.addFoundryExperience(
                experience
        );

        if (completedBatches >= batchCount) {
            clearJob();
            controller.setChanged();
            return;
        }

        /*
         * Begin the next batch from zero on the following server tick.
         */
        progress = 0;

        statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;

        controller.setChanged();
    }

    boolean stop() {
        if (!hasActiveJob()) {
            return false;
        }

        /*
         * Completed batches stay completed. The unfinished current batch has not
         * consumed ingredients, so stopping needs no refund.
         */
        clearJob();
        controller.setChanged();
        return true;
    }

    void cancelAndRefund() {
        if (!hasActiveJob()) {
            return;
        }

        /*
         * Nothing is reserved up front anymore. Unfinished batches therefore have
         * nothing to refund.
         */
        clearJob();
        controller.setChanged();
    }

    private boolean completeCurrentBatch(
            FoundryTankNetwork network
    ) {
        Map<ResourceLocation, Integer> extracted =
                new LinkedHashMap<>();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                requiredIngredients.entrySet()
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

                refund(
                        network,
                        extracted
                );

                statusCode =
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
                        outputMetal,
                        outputAmount
                );

        if (inserted != outputAmount) {
            if (inserted > 0) {
                network.extract(
                        outputMetal,
                        inserted
                );
            }

            refund(
                    network,
                    extracted
            );

            statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;

            controller.setChanged();
            return false;
        }

        return true;
    }

    private static Map<ResourceLocation, Integer> calculateRequirements(
            FoundryAlloyRecipe recipe,
            int batches
    ) {
        Map<ResourceLocation, Integer> result =
                new LinkedHashMap<>();

        for (
                FoundryAlloyIngredient ingredient :
                recipe.ingredients()
        ) {
            int total =
                    safeMultiply(
                            ingredient.amount(),
                            batches
                    );

            if (total <= 0) {
                return Map.of();
            }

            result.merge(
                    ingredient.metal(),
                    total,
                    Integer::sum
            );
        }

        return result;
    }

    private static boolean hasIngredients(
            FoundryTankNetwork network,
            Map<ResourceLocation, Integer> required
    ) {
        for (
                Map.Entry<ResourceLocation, Integer> entry :
                required.entrySet()
        ) {
            if (
                    network.getMoltenAmount(
                            entry.getKey()
                    ) < entry.getValue()
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasCompletionSpace(
            FoundryTankNetwork network,
            int totalInput,
            int totalOutput
    ) {
        long finalTotal =
                (long) network.getTotalMoltenAmount()
                        - totalInput
                        + totalOutput;

        return finalTotal >= 0L
                && finalTotal <= network.getCapacity();
    }

    private static int totalAmount(
            Map<ResourceLocation, Integer> contents
    ) {
        long total = 0L;

        for (Integer amount : contents.values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }

        return total > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) total;
    }

    private static void refund(
            FoundryTankNetwork network,
            Map<ResourceLocation, Integer> contents
    ) {
        for (
                Map.Entry<ResourceLocation, Integer> entry :
                contents.entrySet()
        ) {
            network.insert(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    private static int safeMultiply(
            int first,
            int second
    ) {
        long result =
                (long) first
                        * second;

        return result > Integer.MAX_VALUE
                ? -1
                : (int) result;
    }

    private void clearJob() {
        activeRecipeId = null;
        outputMetal = null;

        requiredIngredients.clear();

        outputAmount = 0;
        batchCount = 0;
        completedBatches = 0;
        progress = 0;
        maxProgress = 1;
        experience = 0;

        statusCode =
                FoundryControllerProcessing.STATUS_READY;
    }

    boolean hasActiveJob() {
        return activeRecipeId != null
                && outputMetal != null
                && outputAmount > 0
                && batchCount > 0
                && completedBatches >= 0
                && completedBatches < batchCount
                && !requiredIngredients.isEmpty();
    }

    int getProgress() {
        return progress;
    }

    int getMaxProgress() {
        return Math.max(
                1,
                maxProgress
        );
    }

    int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the number of batches that have not completed yet.
     *
     * The Controller synchronizes this value to the menu, so the quantity box
     * and the ingredient/output amounts in the recipe preview count down after
     * each completed batch.
     */
    int getBatchCount() {
        return Math.max(
                0,
                batchCount - completedBatches
        );
    }

    @Nullable
    ResourceLocation getOutputMetal() {
        return outputMetal;
    }

    void save(
            CompoundTag tag
    ) {
        if (!hasActiveJob()) {
            return;
        }

        tag.putString(
                "ActiveAlloyRecipe",
                activeRecipeId.toString()
        );

        tag.putString(
                "ActiveAlloyOutput",
                outputMetal.toString()
        );

        tag.putInt(
                "ActiveAlloyOutputAmount",
                outputAmount
        );

        tag.putInt(
                "ActiveAlloyBatchCount",
                batchCount
        );

        tag.putInt(
                "ActiveAlloyCompletedBatches",
                completedBatches
        );

        tag.putInt(
                "ActiveAlloyProgress",
                progress
        );

        tag.putInt(
                "ActiveAlloyMaxProgress",
                maxProgress
        );

        tag.putInt(
                "ActiveAlloyExperience",
                experience
        );

        tag.put(
                REQUIRED_INGREDIENTS_TAG,
                writeAmounts(
                        requiredIngredients
                )
        );
    }

    void load(
            CompoundTag tag
    ) {
        clearJob();

        ResourceLocation recipeId =
                ResourceLocation.tryParse(
                        tag.getString(
                                "ActiveAlloyRecipe"
                        )
                );

        ResourceLocation loadedOutput =
                ResourceLocation.tryParse(
                        tag.getString(
                                "ActiveAlloyOutput"
                        )
                );

        int loadedOutputAmount =
                tag.getInt(
                        "ActiveAlloyOutputAmount"
                );

        if (
                recipeId == null
                        || loadedOutput == null
                        || loadedOutputAmount <= 0
        ) {
            return;
        }

        activeRecipeId = recipeId;
        outputMetal = loadedOutput;
        outputAmount = loadedOutputAmount;

        batchCount =
                Math.max(
                        1,
                        tag.getInt(
                                "ActiveAlloyBatchCount"
                        )
                );

        completedBatches =
                Math.max(
                        0,
                        Math.min(
                                batchCount - 1,
                                tag.getInt(
                                        "ActiveAlloyCompletedBatches"
                                )
                        )
                );

        progress =
                Math.max(
                        0,
                        tag.getInt(
                                "ActiveAlloyProgress"
                        )
                );

        maxProgress =
                Math.max(
                        1,
                        tag.getInt(
                                "ActiveAlloyMaxProgress"
                        )
                );

        progress =
                Math.min(
                        progress,
                        maxProgress
                );

        experience =
                Math.max(
                        0,
                        tag.getInt(
                                "ActiveAlloyExperience"
                        )
                );

        readAmounts(
                tag.getCompound(
                        REQUIRED_INGREDIENTS_TAG
                ),
                requiredIngredients
        );

        if (!hasActiveJob()) {
            clearJob();
            return;
        }

        statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;
    }

    private static CompoundTag writeAmounts(
            Map<ResourceLocation, Integer> contents
    ) {
        CompoundTag result =
                new CompoundTag();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                contents.entrySet()
        ) {
            if (entry.getValue() > 0) {
                result.putInt(
                        entry.getKey().toString(),
                        entry.getValue()
                );
            }
        }

        return result;
    }

    private static void readAmounts(
            CompoundTag source,
            Map<ResourceLocation, Integer> destination
    ) {
        destination.clear();

        for (String key : source.getAllKeys()) {
            ResourceLocation metal =
                    ResourceLocation.tryParse(key);

            int amount =
                    source.getInt(key);

            if (
                    metal != null
                            && amount > 0
                            && ModMoltenMetals.contains(metal)
            ) {
                destination.put(
                        metal,
                        amount
                );
            }
        }
    }
}
