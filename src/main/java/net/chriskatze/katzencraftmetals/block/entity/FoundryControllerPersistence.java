package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

/**
 * Save/load logic for the Foundry Controller.
 *
 * This keeps persistence details out of FoundryControllerBlockEntity.
 */
final class FoundryControllerPersistence {

    private static final String CONTROLLER_ID_TAG =
            "ControllerId";

    private static final String INPUT_INVENTORY_TAG =
            "InputInventory";

    private FoundryControllerPersistence() {
    }

    static void save(
            FoundryControllerBlockEntity controller,
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putString(
                CONTROLLER_ID_TAG,
                controller.getControllerId()
                        .toString()
        );

        controller.getTankStructureForPersistence()
                .save(tag);

        /* Controller-owned molten contents are saved independently of Tanks. */
        controller.getTankStorageForPersistence()
                .save(tag);

        controller.getMetalDiscoveryForPersistence()
                .save(
                        tag
                );

        tag.put(
                INPUT_INVENTORY_TAG,
                controller.getInputInventory()
                        .createTag(
                                registries
                        )
        );

        controller.getFuelSystem()
                .save(
                        tag,
                        registries
                );

        controller.getProcessingForPersistence()
                .save(
                        tag,
                        registries
                );

        controller.getProgressionForPersistence()
                .save(
                        tag
                );

        controller.getAlloyingForPersistence()
                .save(
                        tag
                );
    }

    static void load(
            FoundryControllerBlockEntity controller,
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        controller.setControllerIdForPersistence(
                readControllerId(
                        tag
                )
        );

        controller.getTankStructureForPersistence()
                .load(tag);

        /* Controller storage is the only molten storage source. */
        controller.getTankStorageForPersistence()
                .load(tag);

        controller.getMetalDiscoveryForPersistence()
                .load(
                        tag
                );

        controller.getInputInventory()
                .removeAllItems();

        if (
                tag.contains(
                        INPUT_INVENTORY_TAG,
                        Tag.TAG_LIST
                )
        ) {
            controller.getInputInventory()
                    .fromTag(
                            tag.getList(
                                    INPUT_INVENTORY_TAG,
                                    Tag.TAG_COMPOUND
                            ),
                            registries
                    );
        }

        controller.getFuelSystem()
                .load(
                        tag,
                        registries
                );

        controller.getProgressionForPersistence()
                .load(
                        tag
                );

        controller.getProcessingForPersistence()
                .load(
                        tag,
                        registries
                );

        controller.getAlloyingForPersistence()
                .load(
                        tag
                );
    }

    private static UUID readControllerId(
            CompoundTag tag
    ) {
        if (!tag.contains(CONTROLLER_ID_TAG)) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(
                    tag.getString(
                            CONTROLLER_ID_TAG
                    )
            );
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }
}
