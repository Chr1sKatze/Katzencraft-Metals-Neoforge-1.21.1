package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;

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

        /*
         * IMPORTANT:
         *
         * SimpleContainer#createTag / fromTag are NOT suitable for a machine
         * inventory where the exact slot index matters. They serialize the
         * stacks as a compact list and restore them by adding/merging stacks,
         * which can collapse slot 1 into slot 0, close gaps, etc.
         *
         * ContainerHelper stores the slot index for every non-empty stack.
         * That means the same serialization is safe for:
         *
         * - normal chunk/world saves; and
         * - BlockEntity update tags sent to the client.
         *
         * This directly prevents the Foundry menu from visually merging input
         * slots whenever the Controller sends a client update.
         */
        CompoundTag inputInventoryTag =
                new CompoundTag();

        ContainerHelper.saveAllItems(
                inputInventoryTag,
                controller.getInputInventory()
                        .getItems(),
                registries
        );

        tag.put(
                INPUT_INVENTORY_TAG,
                inputInventoryTag
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
                .clearContent();

        if (
                tag.contains(
                        INPUT_INVENTORY_TAG,
                        Tag.TAG_COMPOUND
                )
        ) {
            /*
             * New slot-preserving format.
             */
            ContainerHelper.loadAllItems(
                    tag.getCompound(
                            INPUT_INVENTORY_TAG
                    ),
                    controller.getInputInventory()
                            .getItems(),
                    registries
            );
        } else if (
                tag.contains(
                        INPUT_INVENTORY_TAG,
                        Tag.TAG_LIST
                )
        ) {
            /*
             * One-time compatibility with existing worlds.
             *
             * Old Controller saves used SimpleContainer#createTag(), which
             * produced a bare list without explicit slot ownership. There is
             * no way to reconstruct gaps that were already lost in an old
             * save, so load it exactly as the old code did once. The next save
             * immediately upgrades it to the slot-preserving CompoundTag
             * format above.
             */
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
