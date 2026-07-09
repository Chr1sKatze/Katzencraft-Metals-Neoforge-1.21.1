package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;

/**
 * Menu synchronization values for the Foundry Controller.
 *
 * Keeping this outside the block entity makes FoundryControllerBlockEntity
 * easier to read without changing the actual synced indices.
 */
final class FoundryControllerMenuData
        implements ContainerData {

    private final FoundryControllerBlockEntity controller;

    FoundryControllerMenuData(
            FoundryControllerBlockEntity controller
    ) {
        this.controller =
                controller;
    }

    @Override
    public int get(
            int index
    ) {
        if (index == 0) {
            return controller.getMenuProgress();
        }

        if (index == 1) {
            return controller.getMenuMaxProgress();
        }

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (
                index >= FoundryControllerBlockEntity.METAL_DATA_START
                        && index < FoundryControllerBlockEntity.SELECTED_METAL_DATA_INDEX
        ) {
            ResourceLocation metal =
                    metalByOffset(
                            index,
                            FoundryControllerBlockEntity.METAL_DATA_START
                    );

            return network != null
                    && metal != null
                    ? network.getMoltenAmount(
                    metal
            )
                    : 0;
        }

        if (index == FoundryControllerBlockEntity.SELECTED_METAL_DATA_INDEX) {
            ResourceLocation selected =
                    network != null
                            ? controller.getSelectedOutputMetalOrDefault(
                            network
                    )
                            : null;

            return selected != null
                    ? ModMoltenMetals.getSyncId(
                    selected
            )
                    : -1;
        }

        if (index == FoundryControllerBlockEntity.TOTAL_AMOUNT_DATA_INDEX) {
            return network != null
                    ? network.getTotalMoltenAmount()
                    : 0;
        }

        if (index == FoundryControllerBlockEntity.CAPACITY_DATA_INDEX) {
            return network != null
                    ? network.getCapacity()
                    : 0;
        }

        if (index == FoundryControllerBlockEntity.BURN_TIME_DATA_INDEX) {
            return controller.getBurnTimeRemaining();
        }

        if (index == FoundryControllerBlockEntity.MAX_BURN_TIME_DATA_INDEX) {
            return controller.getMaxBurnTime();
        }

        if (index == FoundryControllerBlockEntity.TIER_DATA_INDEX) {
            return controller.getFoundryTier();
        }

        if (index == FoundryControllerBlockEntity.EXPERIENCE_DATA_INDEX) {
            return controller.getFoundryExperience();
        }

        if (index == FoundryControllerBlockEntity.TIER_EXPERIENCE_DATA_INDEX) {
            return controller.getFoundryExperienceIntoTier();
        }

        if (index == FoundryControllerBlockEntity.TIER_EXPERIENCE_NEEDED_DATA_INDEX) {
            return controller.getFoundryExperienceNeededForTier();
        }

        if (index == FoundryControllerBlockEntity.ACTIVE_INPUT_SLOT_DATA_INDEX) {
            return controller.getMenuActiveInputSlot();
        }

        if (index == FoundryControllerBlockEntity.STATUS_DATA_INDEX) {
            return controller.getMenuStatusCode();
        }

        if (index == FoundryControllerBlockEntity.ACTIVE_ALLOY_OUTPUT_DATA_INDEX) {
            ResourceLocation output =
                    controller.getActiveAlloyOutputMetal();

            return output == null
                    ? -1
                    : ModMoltenMetals.getSyncId(
                    output
            );
        }

        if (index == FoundryControllerBlockEntity.ALLOY_ACTIVE_DATA_INDEX) {
            return controller.isAlloying()
                    ? 1
                    : 0;
        }

        if (index == FoundryControllerBlockEntity.ACTIVE_ALLOY_BATCH_COUNT_DATA_INDEX) {
            return controller.getActiveAlloyBatchCount();
        }

        if (
                index >= FoundryControllerBlockEntity.DISCOVERED_METAL_DATA_START
                        && index < FoundryControllerBlockEntity.DATA_COUNT
        ) {
            ResourceLocation metal =
                    metalByOffset(
                            index,
                            FoundryControllerBlockEntity.DISCOVERED_METAL_DATA_START
                    );

            return metal != null
                    && controller.hasDiscoveredMoltenMetal(
                    metal
            )
                    ? 1
                    : 0;
        }

        return 0;
    }

    @Override
    public void set(
            int index,
            int value
    ) {
        if (index == 0) {
            controller.setMenuProgressFromData(
                    value
            );
            return;
        }

        if (index == 1) {
            controller.setMenuMaxProgressFromData(
                    value
            );
        }
    }

    @Override
    public int getCount() {
        return FoundryControllerBlockEntity.DATA_COUNT;
    }

    private static ResourceLocation metalByOffset(
            int index,
            int startIndex
    ) {
        int syncId =
                index
                        - startIndex;

        return ModMoltenMetals.bySyncId(
                        syncId
                )
                .map(
                        definition -> definition.id()
                )
                .orElse(null);
    }
}
