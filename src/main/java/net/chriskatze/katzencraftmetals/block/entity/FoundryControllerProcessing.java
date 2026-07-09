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

        FoundryControllerFuelSystem fuel =
                controller.getFuelSystem();

        if (!fuel.hasAvailableFuel()) {
            job.statusCode = STATUS_MISSING_FUEL;
            return;
        }

        if (!fuel.supplyBurnTick()) {
            job.statusCode = STATUS_MISSING_FUEL;
            return;
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
                recipe
        );
    }

    private void selectActiveRecipe(
            int inputSlot,
            FoundryMeltingRecipe recipe
    ) {
        if (job.select(
                inputSlot,
                recipe
        )) {
            controller.setChanged();
        }
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
