package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Output-metal selection rules for the Foundry Controller.
 *
 * The selected value is stored in FoundryMeltingJobState because the selection
 * is persisted with the controller's melting/output state.
 */
final class FoundryOutputSelection {

    private FoundryOutputSelection() {
    }

    @Nullable
    static ResourceLocation get(
            FoundryMeltingJobState job
    ) {
        return job.selectedOutputMetal;
    }

    static boolean set(
            FoundryControllerBlockEntity controller,
            FoundryMeltingJobState job,
            ResourceLocation metal
    ) {
        if (
                metal == null
                        || !ModMoltenMetals.contains(
                        metal
                )
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
                job.selectedOutputMetal,
                metal
        )) {
            return true;
        }

        job.selectedOutputMetal =
                metal;

        controller.setChanged();
        return true;
    }

    @Nullable
    static ResourceLocation getOrDefault(
            FoundryControllerBlockEntity controller,
            FoundryMeltingJobState job,
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

    static boolean normalize(
            FoundryControllerBlockEntity controller,
            FoundryMeltingJobState job,
            FoundryTankNetwork network
    ) {
        ResourceLocation normalized =
                getOrDefault(
                        controller,
                        job,
                        network
                );

        if (Objects.equals(
                job.selectedOutputMetal,
                normalized
        )) {
            return false;
        }

        job.selectedOutputMetal =
                normalized;
        return true;
    }
}
