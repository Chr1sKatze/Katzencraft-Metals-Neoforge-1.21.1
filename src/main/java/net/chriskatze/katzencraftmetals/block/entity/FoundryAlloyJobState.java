package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent state for one active Foundry alloy job.
 *
 * FoundryControllerAlloying owns the behavior/state machine.
 * This class only stores, resets, saves, and loads the job data.
 */
final class FoundryAlloyJobState {

    private static final String REQUIRED_INGREDIENTS_TAG =
            "RequiredAlloyIngredients";

    @Nullable
    ResourceLocation activeRecipeId;

    @Nullable
    ResourceLocation outputMetal;

    /**
     * Ingredients required for exactly one batch.
     */
    final Map<ResourceLocation, Integer> requiredIngredients =
            new LinkedHashMap<>();

    /**
     * Output produced by exactly one completed batch.
     */
    int outputAmount;

    /**
     * Total number of batches requested when the job started.
     */
    int batchCount;

    /**
     * Number of batches that have already completed and produced output.
     */
    int completedBatches;

    /**
     * Progress of the currently running batch only.
     */
    int progress;

    /**
     * Required progress for one batch only.
     */
    int maxProgress = 1;

    /**
     * Experience granted by one completed batch.
     */
    int experience;

    int statusCode =
            FoundryControllerProcessing.STATUS_READY;

    void start(
            ResourceLocation recipeId,
            ResourceLocation outputMetal,
            Map<ResourceLocation, Integer> requiredIngredients,
            int outputAmount,
            int batchCount,
            int maxProgress,
            int experience
    ) {
        this.activeRecipeId = recipeId;
        this.outputMetal = outputMetal;

        this.requiredIngredients.clear();
        this.requiredIngredients.putAll(
                requiredIngredients
        );

        this.outputAmount = outputAmount;
        this.batchCount = batchCount;
        this.completedBatches = 0;
        this.progress = 0;
        this.maxProgress = maxProgress;
        this.experience = experience;

        this.statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;
    }

    void clear() {
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

    int getRemainingBatchCount() {
        return Math.max(
                0,
                batchCount - completedBatches
        );
    }

    int getMaxProgress() {
        return Math.max(
                1,
                maxProgress
        );
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
                FoundryAlloyAmounts.writeAmounts(
                        requiredIngredients
                )
        );
    }

    void load(
            CompoundTag tag
    ) {
        clear();

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

        FoundryAlloyAmounts.readAmounts(
                tag.getCompound(
                        REQUIRED_INGREDIENTS_TAG
                ),
                requiredIngredients
        );

        if (!hasActiveJob()) {
            clear();
            return;
        }

        statusCode =
                FoundryControllerProcessing.STATUS_ALLOYING;
    }
}
