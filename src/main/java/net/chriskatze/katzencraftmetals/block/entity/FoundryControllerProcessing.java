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
 * Recipe progress and output-selection state for one Foundry Controller.
 */
final class FoundryControllerProcessing {

    private final FoundryControllerBlockEntity controller;

    private int progress;

    private int maxProgress =
            FoundryControllerBlockEntity.MAX_PROGRESS;

    @Nullable
    private ResourceLocation activeMoltenMetal;

    private int activeMoltenAmount;

    private int activeInputSlot = -1;

    @Nullable
    private ResourceLocation selectedOutputMetal;

    FoundryControllerProcessing(
            FoundryControllerBlockEntity controller
    ) {
        this.controller =
                controller;
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

    @Nullable
    ResourceLocation getSelectedOutputMetal() {
        return selectedOutputMetal;
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
                        || network.getMoltenAmount(
                        metal
                ) <= 0
        ) {
            return false;
        }

        if (Objects.equals(
                selectedOutputMetal,
                metal
        )) {
            return true;
        }

        selectedOutputMetal =
                metal;

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
                selectedOutputMetal != null
                        && network.getMoltenAmount(
                        selectedOutputMetal
                ) > 0
        ) {
            return selectedOutputMetal;
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
                getSelectedOutputMetalOrDefault(
                        network
                );

        if (!Objects.equals(
                selectedOutputMetal,
                normalized
        )) {
            selectedOutputMetal =
                    normalized;

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
            clearActiveRecipe();
            return;
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            clearActiveRecipe();
            return;
        }

        network.ensureMoltenContentsMigrated();

        normalizeSelectedOutputMetal(
                network
        );

        if (level.getGameTime() % 20L == 0L) {
            controller.getFuelSystem()
                    .migrateNearbyLegacyFuelChambers(
                            network
                    );
        }

        int inputSlot =
                findNextInputSlot();

        if (inputSlot < 0) {
            clearActiveRecipe();
            return;
        }

        ItemStack inputStack =
                controller.getInputInventory()
                        .getItem(
                                inputSlot
                        );

        Optional<FoundryMeltingRecipe> recipeOptional =
                findMeltingRecipe(
                        inputStack
                );

        if (recipeOptional.isEmpty()) {
            clearActiveRecipe();
            return;
        }

        FoundryMeltingRecipe recipe =
                recipeOptional.get();

        selectActiveRecipe(
                inputSlot,
                recipe
        );

        /*
         * Queue order is strict. If the first valid queued item cannot fit,
         * later slots are not skipped and no fuel is consumed.
         */
        if (
                !network.canAccept(
                        recipe.moltenMetal(),
                        recipe.moltenAmount()
                )
        ) {
            return;
        }

        if (
                !controller.getFuelSystem()
                        .supplyBurnTick()
        ) {
            return;
        }

        progress++;

        if (progress >= maxProgress) {
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
        return activeInputSlot == inputSlot
                && Objects.equals(
                activeMoltenMetal,
                recipe.moltenMetal()
        )
                && activeMoltenAmount
                == recipe.moltenAmount()
                && maxProgress
                == recipe.processingTime();
    }

    private void selectActiveRecipe(
            int inputSlot,
            FoundryMeltingRecipe recipe
    ) {
        if (matchesActiveRecipe(inputSlot, recipe)) {
            return;
        }

        progress = 0;
        activeInputSlot = inputSlot;
        activeMoltenMetal = recipe.moltenMetal();
        activeMoltenAmount = recipe.moltenAmount();
        maxProgress = recipe.processingTime();

        controller.setChanged();
    }

    private void clearActiveRecipe() {
        boolean changed =
                progress != 0
                        || maxProgress
                        != FoundryControllerBlockEntity.MAX_PROGRESS
                        || activeInputSlot != -1
                        || activeMoltenMetal != null
                        || activeMoltenAmount != 0;

        progress = 0;
        maxProgress = FoundryControllerBlockEntity.MAX_PROGRESS;
        activeInputSlot = -1;
        activeMoltenMetal = null;
        activeMoltenAmount = 0;

        if (changed) {
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
                        .getItem(
                                expectedInputSlot
                        );

        Optional<FoundryMeltingRecipe> currentRecipeOptional =
                findMeltingRecipe(
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

        if (selectedOutputMetal == null) {
            selectedOutputMetal =
                    currentRecipe.moltenMetal();
        }

        inputStack.shrink(1);
        progress = 0;

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
                .equals(
                        second.moltenMetal()
                );
    }

    private void resetProgress() {
        if (progress == 0) {
            return;
        }

        progress =
                0;

        controller.setChanged();
    }

    int getProgress() {
        return progress;
    }

    int getMaxProgress() {
        return maxProgress;
    }

    void setProgressFromMenuData(
            int value
    ) {
        progress =
                Math.max(
                        0,
                        value
                );
    }

    void setMaxProgressFromMenuData(
            int value
    ) {
        maxProgress =
                Math.max(
                        1,
                        value
                );
    }

    @Nullable
    ResourceLocation getActiveMoltenMetal() {
        return activeMoltenMetal;
    }

    int getActiveMoltenAmount() {
        return activeMoltenAmount;
    }

    int getActiveInputSlot() {
        return activeInputSlot;
    }

    void save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putInt(
                "Progress",
                progress
        );

        tag.putInt(
                "MaxProgress",
                maxProgress
        );

        if (activeMoltenMetal != null) {
            tag.putString(
                    "ActiveMoltenMetal",
                    activeMoltenMetal.toString()
            );
        }

        tag.putInt(
                "ActiveMoltenAmount",
                activeMoltenAmount
        );

        tag.putInt(
                "ActiveInputSlot",
                activeInputSlot
        );

        if (selectedOutputMetal != null) {
            tag.putString(
                    "SelectedOutputMetal",
                    selectedOutputMetal.toString()
            );
        }
    }

    void load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        maxProgress =
                tag.contains("MaxProgress")
                        ? Math.max(
                        1,
                        tag.getInt(
                                "MaxProgress"
                        )
                )
                        : FoundryControllerBlockEntity.MAX_PROGRESS;

        progress =
                Math.max(
                        0,
                        Math.min(
                                tag.getInt(
                                        "Progress"
                                ),
                                maxProgress
                        )
                );

        activeMoltenMetal =
                null;

        if (tag.contains("ActiveMoltenMetal")) {
            activeMoltenMetal =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "ActiveMoltenMetal"
                            )
                    );
        }

        activeMoltenAmount =
                Math.max(
                        0,
                        tag.getInt(
                                "ActiveMoltenAmount"
                        )
                );

        activeInputSlot =
                tag.contains("ActiveInputSlot")
                        ? tag.getInt("ActiveInputSlot")
                        : (activeMoltenMetal != null ? 0 : -1);

        if (
                activeInputSlot < 0
                        || activeInputSlot
                        >= FoundryControllerBlockEntity.INPUT_SLOT_COUNT
        ) {
            activeInputSlot = -1;
        }

        selectedOutputMetal =
                null;

        if (tag.contains("SelectedOutputMetal")) {
            ResourceLocation parsedSelection =
                    ResourceLocation.tryParse(
                            tag.getString(
                                    "SelectedOutputMetal"
                            )
                    );

            if (
                    parsedSelection != null
                            && ModMoltenMetals.contains(
                            parsedSelection
                    )
            ) {
                selectedOutputMetal =
                        parsedSelection;
            }
        }

        if (
                activeMoltenMetal != null
                        && !ModMoltenMetals.contains(
                        activeMoltenMetal
                )
        ) {
            activeMoltenMetal =
                    null;

            activeMoltenAmount =
                    0;

            activeInputSlot =
                    -1;

            progress =
                    0;

            maxProgress =
                    FoundryControllerBlockEntity.MAX_PROGRESS;
        }
    }
}
