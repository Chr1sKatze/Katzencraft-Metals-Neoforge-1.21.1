package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Persistent state for the Controller's current melting job.
 *
 * FoundryControllerProcessing owns the behavior/state machine.
 * This class only stores, resets, saves, and loads the melting data.
 */
final class FoundryMeltingJobState {

    int progress;

    int maxProgress =
            FoundryControllerBlockEntity.MAX_PROGRESS;

    @Nullable
    ResourceLocation activeMoltenMetal;

    int activeMoltenAmount;

    int activeInputSlot = -1;

    int statusCode =
            FoundryControllerProcessing.STATUS_READY;

    @Nullable
    ResourceLocation selectedOutputMetal;

    boolean matches(
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

    boolean select(
            int inputSlot,
            FoundryMeltingRecipe recipe
    ) {
        if (matches(
                inputSlot,
                recipe
        )) {
            return false;
        }

        progress = 0;
        activeInputSlot = inputSlot;
        activeMoltenMetal = recipe.moltenMetal();
        activeMoltenAmount = recipe.moltenAmount();
        maxProgress = recipe.processingTime();

        return true;
    }

    boolean clearActiveRecipe() {
        boolean changed =
                progress != 0
                        || maxProgress
                        != FoundryControllerBlockEntity.MAX_PROGRESS
                        || activeInputSlot != -1
                        || activeMoltenMetal != null
                        || activeMoltenAmount != 0;

        progress = 0;
        maxProgress =
                FoundryControllerBlockEntity.MAX_PROGRESS;
        activeInputSlot = -1;
        activeMoltenMetal = null;
        activeMoltenAmount = 0;

        return changed;
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

        tag.putInt(
                "ProcessingStatus",
                statusCode
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
                        tag.getInt("MaxProgress")
                )
                        : FoundryControllerBlockEntity.MAX_PROGRESS;

        progress =
                Math.max(
                        0,
                        Math.min(
                                tag.getInt("Progress"),
                                maxProgress
                        )
                );

        activeMoltenMetal = null;

        if (tag.contains("ActiveMoltenMetal")) {
            activeMoltenMetal =
                    ResourceLocation.tryParse(
                            tag.getString("ActiveMoltenMetal")
                    );
        }

        activeMoltenAmount =
                Math.max(
                        0,
                        tag.getInt("ActiveMoltenAmount")
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

        selectedOutputMetal = null;

        if (tag.contains("SelectedOutputMetal")) {
            ResourceLocation parsedSelection =
                    ResourceLocation.tryParse(
                            tag.getString("SelectedOutputMetal")
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
            activeMoltenMetal = null;
            activeMoltenAmount = 0;
            activeInputSlot = -1;
            progress = 0;
            maxProgress =
                    FoundryControllerBlockEntity.MAX_PROGRESS;
        }

        statusCode =
                activeMoltenMetal != null
                        && activeInputSlot >= 0
                        ? FoundryControllerProcessing.STATUS_MELTING
                        : FoundryControllerProcessing.STATUS_READY;
    }
}
