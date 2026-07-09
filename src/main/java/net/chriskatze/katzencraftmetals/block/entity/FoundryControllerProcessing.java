package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
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
        return findMeltingRecipe(stack)
                .isPresent();
    }

    private Optional<FoundryMeltingRecipe> findMeltingRecipe(
            ItemStack stack
    ) {
        if (
                controller.getLevel() == null
                        || stack.isEmpty()
        ) {
            return Optional.empty();
        }

        return controller.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                        new SingleRecipeInput(stack),
                        controller.getLevel()
                )
                .map(holder -> holder.value())
                .filter(
                        recipe ->
                                ModMoltenMetals.contains(
                                        recipe.moltenMetal()
                                )
                );
    }

    @Nullable
    ResourceLocation getSelectedOutputMetal() {
        return job.selectedOutputMetal;
    }

    boolean setSelectedOutputMetal(
            ResourceLocation metal
    ) {
        if (
                metal == null
                        || !ModMoltenMetals.contains(metal)
        ) {
            return false;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (
                controller.getLevel() == null
                        || network == null
                        || network.getMoltenAmount(metal) <= 0
        ) {
            return false;
        }

        if (Objects.equals(
                job.selectedOutputMetal,
                metal
        )) {
            return true;
        }

        job.selectedOutputMetal = metal;
        controller.setChanged();
        return true;
    }

    @Nullable
    ResourceLocation getSelectedOutputMetalOrDefault(
            FoundryTankNetwork network
    ) {
        if (
                controller.getLevel() == null
                        || network == null
        ) {
            return null;
        }

        if (
                job.selectedOutputMetal != null
                        && network.getMoltenAmount(
                        job.selectedOutputMetal
                ) > 0
        ) {
            return job.selectedOutputMetal;
        }

        for (
                var definition :
                ModMoltenMetals.lightestFirst()
        ) {
            if (
                    network.getMoltenAmount(
                            definition.id()
                    ) > 0
            ) {
                return definition.id();
            }
        }

        return null;
    }

    private void normalizeSelectedOutputMetal(
            FoundryTankNetwork network
    ) {
        ResourceLocation normalized =
                getSelectedOutputMetalOrDefault(network);

        if (!Objects.equals(
                job.selectedOutputMetal,
                normalized
        )) {
            job.selectedOutputMetal = normalized;
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
                findMeltingRecipe(inputStack);

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
                            && findMeltingRecipe(stack).isPresent()
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
                findMeltingRecipe(inputStack);

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
                        || !sameRecipeResult(
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

    private static boolean sameRecipeResult(
            FoundryMeltingRecipe first,
            FoundryMeltingRecipe second
    ) {
        return first.moltenAmount()
                == second.moltenAmount()
                && first.processingTime()
                == second.processingTime()
                && first.moltenMetal()
                .equals(second.moltenMetal());
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
