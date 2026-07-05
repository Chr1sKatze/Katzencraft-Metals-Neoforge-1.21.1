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
 * Ingredient metals remain in the tank while progress is running. They are
 * consumed atomically only when the alloy job completes successfully.
 */
final class FoundryControllerAlloying {

    private static final String REQUIRED_INGREDIENTS_TAG =
            "RequiredAlloyIngredients";

    private static final String LEGACY_RESERVED_INGREDIENTS_TAG =
            "LegacyReservedAlloyIngredients";

    private final FoundryControllerBlockEntity controller;

    @Nullable
    private ResourceLocation activeRecipeId;

    @Nullable
    private ResourceLocation outputMetal;

    private final Map<ResourceLocation, Integer> requiredIngredients =
            new LinkedHashMap<>();

    /**
     * Compatibility for worlds saved by the old implementation, which removed
     * ingredients as soon as a job started. These amounts are restored once the
     * tank network becomes available.
     */
    private final Map<ResourceLocation, Integer> legacyReservedIngredients =
            new LinkedHashMap<>();

    private int outputAmount;
    private int batchCount;
    private int progress;
    private int maxProgress = 1;
    private int experience;

    private int statusCode =
            FoundryControllerProcessing.STATUS_READY;

    FoundryControllerAlloying(
            FoundryControllerBlockEntity controller
    ) {
        this.controller = controller;
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

        Map<ResourceLocation, Integer> required =
                calculateRequirements(
                        recipe,
                        requestedBatches
                );

        if (
                required.isEmpty()
                        || !hasIngredients(
                        network,
                        required
                )
        ) {
            return false;
        }

        int totalInput =
                totalAmount(required);

        int totalOutput =
                safeMultiply(
                        recipe.outputAmount(),
                        requestedBatches
                );

        int totalProgress =
                safeMultiply(
                        recipe.processingTime(),
                        requestedBatches
                );

        int totalExperience =
                safeMultiply(
                        recipe.experience(),
                        requestedBatches
                );

        if (
                totalInput <= 0
                        || totalOutput <= 0
                        || totalProgress <= 0
                        || totalExperience < 0
        ) {
            return false;
        }

        if (
                !hasCompletionSpace(
                        network,
                        totalInput,
                        totalOutput
                )
        ) {
            return false;
        }

        activeRecipeId =
                holder.id();

        outputMetal =
                recipe.outputMetal();

        requiredIngredients.clear();
        requiredIngredients.putAll(required);

        legacyReservedIngredients.clear();

        outputAmount =
                totalOutput;

        batchCount =
                requestedBatches;

        progress = 0;
        maxProgress = totalProgress;
        experience = totalExperience;

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

        if (!restoreLegacyReservation(network)) {
            statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;
            controller.setChanged();
            return;
        }

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
         * Progress consumes fuel, but it does not consume ingredient metals.
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
         * At 100%, wait until the required metals and final tank space are
         * available. No additional fuel is consumed while waiting.
         */
        if (!hasIngredients(
                network,
                requiredIngredients
        )) {
            statusCode =
                    FoundryControllerProcessing.STATUS_ALLOYING;
            return;
        }

        int totalInput =
                totalAmount(requiredIngredients);

        if (
                !hasCompletionSpace(
                        network,
                        totalInput,
                        outputAmount
                )
        ) {
            statusCode =
                    FoundryControllerProcessing.STATUS_TANK_FULL;
            return;
        }

        if (!completeJob(network)) {
            return;
        }

        controller.addFoundryExperience(experience);
        clearJob();
        controller.setChanged();
    }

    boolean stop() {
        if (!hasActiveJob()) {
            return false;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (
                !legacyReservedIngredients.isEmpty()
                        && (
                        network == null
                                || !restoreLegacyReservation(network)
                )
        ) {
            return false;
        }

        clearJob();
        controller.setChanged();
        return true;
    }

    void cancelAndRefund() {
        if (!hasActiveJob()) {
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network != null) {
            restoreLegacyReservation(network);
        }

        clearJob();
        controller.setChanged();
    }

    private boolean completeJob(
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

    private boolean restoreLegacyReservation(
            FoundryTankNetwork network
    ) {
        if (legacyReservedIngredients.isEmpty()) {
            return true;
        }

        var iterator =
                legacyReservedIngredients
                        .entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry =
                    iterator.next();

            int inserted =
                    network.insert(
                            entry.getKey(),
                            entry.getValue()
                    );

            if (inserted <= 0) {
                return false;
            }

            int remaining =
                    entry.getValue() - inserted;

            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
                return false;
            }
        }

        controller.setChanged();
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
        legacyReservedIngredients.clear();

        outputAmount = 0;
        batchCount = 0;
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

    int getBatchCount() {
        return batchCount;
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
                writeAmounts(requiredIngredients)
        );

        if (!legacyReservedIngredients.isEmpty()) {
            tag.put(
                    LEGACY_RESERVED_INGREDIENTS_TAG,
                    writeAmounts(
                            legacyReservedIngredients
                    )
            );
        }
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

        if (tag.contains(REQUIRED_INGREDIENTS_TAG)) {
            readAmounts(
                    tag.getCompound(
                            REQUIRED_INGREDIENTS_TAG
                    ),
                    requiredIngredients
            );
        } else {
            /*
             * Old saves used this tag for metals that had already been removed
             * at job start. Load them as the new requirements and restore them.
             */
            CompoundTag oldReserved =
                    tag.getCompound(
                            "ReservedAlloyIngredients"
                    );

            readAmounts(
                    oldReserved,
                    requiredIngredients
            );

            legacyReservedIngredients.putAll(
                    requiredIngredients
            );
        }

        if (
                tag.contains(
                        LEGACY_RESERVED_INGREDIENTS_TAG
                )
        ) {
            readAmounts(
                    tag.getCompound(
                            LEGACY_RESERVED_INGREDIENTS_TAG
                    ),
                    legacyReservedIngredients
            );
        }

        if (requiredIngredients.isEmpty()) {
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
