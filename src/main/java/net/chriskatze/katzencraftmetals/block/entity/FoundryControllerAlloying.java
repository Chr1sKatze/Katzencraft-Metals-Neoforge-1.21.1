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

/** One persistent, explicitly started alloy job owned by a Controller. */
final class FoundryControllerAlloying {

    static final int STATUS_HEATING_FOR_ALLOY = 7;
    static final int STATUS_ALLOYING = 8;

    private final FoundryControllerBlockEntity controller;

    @Nullable
    private ResourceLocation activeRecipeId;

    @Nullable
    private ResourceLocation outputMetal;

    private final Map<ResourceLocation, Integer> reservedIngredients =
            new LinkedHashMap<>();

    private int outputAmount;
    private int batchCount;
    private int progress;
    private int maxProgress = 1;
    private int requiredTemperature;
    private int experience;
    private int statusCode = FoundryControllerProcessing.STATUS_READY;

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
                recipe.requiredTier() > controller.getFoundryTier()
                        || !ModMoltenMetals.contains(recipe.outputMetal())
                        || recipe.requiredTemperature()
                        > controller.getMaximumTemperature()
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

        network = controller.getOwnedTankNetwork();

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
                required.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

        int totalOutput =
                safeMultiply(
                        recipe.outputAmount(),
                        requestedBatches
                );

        if (totalOutput <= 0) {
            return false;
        }

        int finalTotal =
                network.getTotalMoltenAmount()
                        - totalInput
                        + totalOutput;

        if (finalTotal > network.getCapacity()) {
            return false;
        }

        Map<ResourceLocation, Integer> extracted =
                new LinkedHashMap<>();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                required.entrySet()
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

                return false;
            }

            extracted.put(
                    entry.getKey(),
                    amount
            );
        }

        activeRecipeId = holder.id();
        outputMetal = recipe.outputMetal();
        reservedIngredients.clear();
        reservedIngredients.putAll(extracted);
        outputAmount = totalOutput;
        batchCount = requestedBatches;
        progress = 0;
        maxProgress = safeMultiply(
                recipe.processingTime(),
                requestedBatches
        );
        requiredTemperature = recipe.requiredTemperature();
        experience = safeMultiply(
                recipe.experience(),
                requestedBatches
        );
        statusCode = controller.getTemperatureSystem()
                .isHotEnough(requiredTemperature)
                ? STATUS_ALLOYING
                : STATUS_HEATING_FOR_ALLOY;

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
            statusCode = FoundryControllerProcessing.STATUS_NO_TANKS;
            controller.getTemperatureSystem().coolTick();
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            statusCode = FoundryControllerProcessing.STATUS_NO_TANKS;
            controller.getTemperatureSystem().coolTick();
            return;
        }

        if (
                FoundryAlloyCatalog.byId(
                        level,
                        activeRecipeId
                ).isEmpty()
        ) {
            cancelAndRefund();
            return;
        }

        if (
                !network.canAccept(
                        outputMetal,
                        outputAmount
                )
        ) {
            statusCode = FoundryControllerProcessing.STATUS_TANK_FULL;
            controller.getTemperatureSystem().coolTick();
            return;
        }

        FoundryControllerFuelSystem fuel =
                controller.getFuelSystem();

        if (!fuel.hasAvailableFuel()) {
            statusCode = FoundryControllerProcessing.STATUS_MISSING_FUEL;
            controller.getTemperatureSystem().coolTick();
            return;
        }

        if (!fuel.canReachTemperature(requiredTemperature)) {
            statusCode =
                    FoundryControllerProcessing
                            .STATUS_TEMPERATURE_TOO_LOW;

            controller.getTemperatureSystem().coolTick();
            return;
        }

        if (!fuel.supplyBurnTick(requiredTemperature)) {
            statusCode = FoundryControllerProcessing.STATUS_MISSING_FUEL;
            controller.getTemperatureSystem().coolTick();
            return;
        }

        controller.getTemperatureSystem().heatTick(
                fuel.getActiveFuelMaximumTemperature()
        );

        if (
                !controller.getTemperatureSystem()
                        .isHotEnough(requiredTemperature)
        ) {
            statusCode = STATUS_HEATING_FOR_ALLOY;
            controller.setChanged();
            return;
        }

        statusCode = STATUS_ALLOYING;
        progress++;

        if (progress < maxProgress) {
            controller.setChanged();
            return;
        }

        int inserted =
                network.insert(
                        outputMetal,
                        outputAmount
                );

        if (inserted != outputAmount) {
            statusCode = FoundryControllerProcessing.STATUS_TANK_FULL;
            return;
        }

        controller.addFoundryExperience(experience);
        clearJob();
        controller.setChanged();
    }

    void cancelAndRefund() {
        if (!hasActiveJob()) {
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network != null) {
            refund(
                    network,
                    reservedIngredients
            );
        }

        clearJob();
        controller.setChanged();
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
        reservedIngredients.clear();
        outputAmount = 0;
        batchCount = 0;
        progress = 0;
        maxProgress = 1;
        requiredTemperature = 0;
        experience = 0;
        statusCode = FoundryControllerProcessing.STATUS_READY;
    }

    boolean hasActiveJob() {
        return activeRecipeId != null
                && outputMetal != null
                && outputAmount > 0;
    }

    int getProgress() {
        return progress;
    }

    int getMaxProgress() {
        return Math.max(1, maxProgress);
    }

    int getRequiredTemperature() {
        return requiredTemperature;
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
                "ActiveAlloyRequiredTemperature",
                requiredTemperature
        );

        tag.putInt(
                "ActiveAlloyExperience",
                experience
        );

        CompoundTag reserved =
                new CompoundTag();

        for (
                Map.Entry<ResourceLocation, Integer> entry :
                reservedIngredients.entrySet()
        ) {
            reserved.putInt(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        tag.put(
                "ReservedAlloyIngredients",
                reserved
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
        batchCount = Math.max(
                1,
                tag.getInt(
                        "ActiveAlloyBatchCount"
                )
        );
        progress = Math.max(
                0,
                tag.getInt(
                        "ActiveAlloyProgress"
                )
        );
        maxProgress = Math.max(
                1,
                tag.getInt(
                        "ActiveAlloyMaxProgress"
                )
        );
        progress = Math.min(
                progress,
                maxProgress
        );
        requiredTemperature = Math.max(
                0,
                tag.getInt(
                        "ActiveAlloyRequiredTemperature"
                )
        );
        experience = Math.max(
                0,
                tag.getInt(
                        "ActiveAlloyExperience"
                )
        );

        CompoundTag reserved =
                tag.getCompound(
                        "ReservedAlloyIngredients"
                );

        for (String key : reserved.getAllKeys()) {
            ResourceLocation metal =
                    ResourceLocation.tryParse(key);

            int amount =
                    reserved.getInt(key);

            if (
                    metal != null
                            && amount > 0
            ) {
                reservedIngredients.put(
                        metal,
                        amount
                );
            }
        }

        if (reservedIngredients.isEmpty()) {
            clearJob();
            return;
        }

        statusCode = STATUS_HEATING_FOR_ALLOY;
    }
}
