package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Recipe progress and output-selection behavior for one Foundry Controller.
 */
final class FoundryControllerProcessing {

    static final int STATUS_READY = 0;
    static final int STATUS_NO_TANKS = 1;
    static final int STATUS_TANK_FULL = 2;
    static final int STATUS_MISSING_FUEL = 3;
    static final int STATUS_MELTING = 4;
    static final int STATUS_ALLOYING = 5;

    private static final int BASE_SPEED_PERCENT = 100;
    private static final int SPEED_PERCENT_PER_TIER = 25;

    private final FoundryControllerBlockEntity controller;

    private final FoundryMeltingJobState job =
            new FoundryMeltingJobState();

    FoundryControllerProcessing(
            FoundryControllerBlockEntity controller
    ) {
        this.controller = controller;
    }

    boolean canMelt(
            ItemStack stack
    ) {
        return FoundryMeltingRecipes.canMelt(
                controller.getLevel(),
                stack
        );
    }

    @Nullable
    ResourceLocation getSelectedOutputMetal() {
        return FoundryOutputSelection.get(
                job
        );
    }

    boolean setSelectedOutputMetal(
            ResourceLocation metal
    ) {
        return FoundryOutputSelection.set(
                controller,
                job,
                metal
        );
    }

    @Nullable
    ResourceLocation getSelectedOutputMetalOrDefault(
            FoundryTankNetwork network
    ) {
        return FoundryOutputSelection.getOrDefault(
                controller,
                job,
                network
        );
    }

    private void normalizeSelectedOutputMetal(
            FoundryTankNetwork network
    ) {
        if (
                FoundryOutputSelection.normalize(
                        controller,
                        job,
                        network
                )
        ) {
            controller.setChanged();
        }
    }

    void tick(
            Level level,
            BlockPos pos
    ) {
        if (level.isClientSide()) {
            return;
        }

        FoundryControllerFuelSystem fuel =
                controller.getFuelSystem();

        /*
         * Existing burn time keeps ticking down even if melting cannot currently
         * continue. Starting a new burn cycle still only happens later after a
         * valid input and enough tank space have been found.
         */
        fuel.tickBurnTime();

        if (!controller.ensureTankNetwork()) {
            job.statusCode = STATUS_NO_TANKS;
            clearActiveRecipe();
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            job.statusCode = STATUS_NO_TANKS;
            clearActiveRecipe();
            return;
        }

        network.ensureMoltenContentsMigrated();
        normalizeSelectedOutputMetal(network);

        if (level.getGameTime() % 20L == 0L) {
            controller.getFuelSystem()
                    .migrateNearbyLegacyFuelChambers(network);
        }

        int inputSlot =
                findNextInputSlot();

        if (inputSlot < 0) {
            job.statusCode = STATUS_READY;
            clearActiveRecipe();
            return;
        }

        ItemStack inputStack =
                controller.getInputInventory()
                        .getItem(inputSlot);

        Optional<FoundryMeltingRecipe> recipeOptional =
                FoundryMeltingRecipes.find(
                        controller.getLevel(),
                        inputStack
                );

        if (recipeOptional.isEmpty()) {
            job.statusCode = STATUS_READY;
            clearActiveRecipe();
            return;
        }

        FoundryMeltingRecipe recipe =
                recipeOptional.get();

        selectActiveRecipe(
                inputSlot,
                recipe
        );

        if (
                !network.canAccept(
                        recipe.moltenMetal(),
                        recipe.moltenAmount()
                )
        ) {
            job.statusCode = STATUS_TANK_FULL;
            return;
        }

        if (!fuel.isBurning()) {
            if (!fuel.hasAvailableFuel()) {
                job.statusCode = STATUS_MISSING_FUEL;
                clearActiveRecipe();
                return;
            }

            if (!fuel.tryStartBurnCycle()) {
                job.statusCode = STATUS_MISSING_FUEL;
                clearActiveRecipe();
                return;
            }
        }

        job.statusCode = STATUS_MELTING;
        job.progress++;

        if (job.progress >= job.maxProgress) {
            finishMelting(
                    network,
                    inputSlot,
                    recipe
            );
        }

        controller.setChanged();
    }

    private int findNextInputSlot() {
        for (
                int slot = 0;
                slot < controller.getUnlockedInputSlotCount();
                slot++
        ) {
            ItemStack stack =
                    controller.getInputInventory()
                            .getItem(slot);

            if (
                    !stack.isEmpty()
                            && FoundryMeltingRecipes.canMelt(
                            controller.getLevel(),
                            stack
                    )
            ) {
                return slot;
            }
        }

        return -1;
    }

    private boolean matchesActiveRecipe(
            int inputSlot,
            FoundryMeltingRecipe recipe
    ) {
        return job.matches(
                inputSlot,
                recipe,
                getTierAdjustedProcessingTime(
                        recipe
                )
        );
    }

    private void selectActiveRecipe(
            int inputSlot,
            FoundryMeltingRecipe recipe
    ) {
        if (job.select(
                inputSlot,
                recipe,
                getTierAdjustedProcessingTime(
                        recipe
                )
        )) {
            controller.setChanged();
        }
    }

    private int getTierAdjustedProcessingTime(
            FoundryMeltingRecipe recipe
    ) {
        int tier =
                Math.max(
                        1,
                        controller.getFoundryTier()
                );

        int speedPercent =
                BASE_SPEED_PERCENT
                        + (tier - 1)
                        * SPEED_PERCENT_PER_TIER;

        long scaledProcessingTime =
                (long) recipe.processingTime()
                        * BASE_SPEED_PERCENT;

        /*
         * 25% faster means 125% work speed, not "remove 25% time".
         *
         * Example with an iron recipe of 100 ticks:
         * Tier 1: 100 / 1.00 = 100 ticks
         * Tier 2: 100 / 1.25 = 80 ticks
         * Tier 3: 100 / 1.50 = 67 ticks
         * Tier 4: 100 / 1.75 = 58 ticks
         */
        return Math.max(
                1,
                (int) (
                        (
                                scaledProcessingTime
                                        + speedPercent
                                        - 1L
                        )
                                / speedPercent
                )
        );
    }

    private void clearActiveRecipe() {
        if (job.clearActiveRecipe()) {
            controller.setChanged();
        }
    }

    private void finishMelting(
            FoundryTankNetwork network,
            int expectedInputSlot,
            FoundryMeltingRecipe expectedRecipe
    ) {
        if (
                !controller.isInputSlotUnlocked(
                        expectedInputSlot
                )
        ) {
            clearActiveRecipe();
            return;
        }

        ItemStack inputStack =
                controller.getInputInventory()
                        .getItem(expectedInputSlot);

        Optional<FoundryMeltingRecipe> currentRecipeOptional =
                FoundryMeltingRecipes.find(
                        controller.getLevel(),
                        inputStack
                );

        if (currentRecipeOptional.isEmpty()) {
            clearActiveRecipe();
            return;
        }

        FoundryMeltingRecipe currentRecipe =
                currentRecipeOptional.get();

        if (
                !matchesActiveRecipe(
                        expectedInputSlot,
                        currentRecipe
                )
                        || !FoundryMeltingRecipes.sameResult(
                        expectedRecipe,
                        currentRecipe
                )
        ) {
            selectActiveRecipe(
                    expectedInputSlot,
                    currentRecipe
            );
            return;
        }

        if (
                controller.getLevel() == null
                        || !network.canAccept(
                        currentRecipe.moltenMetal(),
                        currentRecipe.moltenAmount()
                )
        ) {
            return;
        }

        int inserted =
                network.insert(
                        currentRecipe.moltenMetal(),
                        currentRecipe.moltenAmount()
                );

        if (
                inserted
                        != currentRecipe.moltenAmount()
        ) {
            return;
        }

        if (job.selectedOutputMetal == null) {
            job.selectedOutputMetal =
                    currentRecipe.moltenMetal();
        }

        inputStack.shrink(1);
        job.progress = 0;

        controller.addFoundryExperience(1);

        controller.getInputInventory()
                .setChanged();

        controller.setChanged();
    }

    int getProgress() {
        return job.progress;
    }

    int getMaxProgress() {
        return job.maxProgress;
    }

    void setProgressFromMenuData(
            int value
    ) {
        job.setProgressFromMenuData(
                value
        );
    }

    void setMaxProgressFromMenuData(
            int value
    ) {
        job.setMaxProgressFromMenuData(
                value
        );
    }

    @Nullable
    ResourceLocation getActiveMoltenMetal() {
        return job.activeMoltenMetal;
    }

    int getActiveMoltenAmount() {
        return job.activeMoltenAmount;
    }

    int getActiveInputSlot() {
        return job.activeInputSlot;
    }

    int getStatusCode() {
        return job.statusCode;
    }

    void save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        job.save(
                tag,
                registries
        );
    }

    void load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        job.load(
                tag,
                registries
        );
    }
}
