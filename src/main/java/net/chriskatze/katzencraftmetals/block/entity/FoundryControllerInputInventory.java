package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Input inventory for the Foundry Controller.
 *
 * Slot unlock checks and meltability checks belong here, so the Controller
 * does not need an anonymous SimpleContainer implementation.
 */
final class FoundryControllerInputInventory
        extends SimpleContainer {

    private final FoundryControllerBlockEntity controller;

    FoundryControllerInputInventory(
            FoundryControllerBlockEntity controller
    ) {
        super(
                FoundryControllerBlockEntity.INPUT_SLOT_COUNT
        );

        this.controller =
                controller;
    }

    @Override
    public boolean canPlaceItem(
            int slot,
            ItemStack stack
    ) {
        return controller.isInputSlotUnlocked(
                slot
        )
                && controller.canMelt(
                stack
        );
    }

    @Override
    public void setChanged() {
        super.setChanged();
        controller.setChanged();
    }
}
