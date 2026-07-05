package net.chriskatze.katzencraftmetals.fuel;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One fuel type understood by the Foundry Controller. */
public record FoundryFuelDefinition(
        Item item,
        int burnTime
) {

    public FoundryFuelDefinition {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Foundry fuel item cannot be null."
            );
        }

        if (burnTime <= 0) {
            throw new IllegalArgumentException(
                    "Foundry fuel burn time must be positive."
            );
        }
    }

    public boolean matches(
            ItemStack stack
    ) {
        return stack != null
                && !stack.isEmpty()
                && stack.is(item);
    }
}
