package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;

/**
 * One bottom-to-top local molten-metal segment inside a Foundry Tank.
 */
public record FoundryMetalLayer(
        ResourceLocation metal,
        int amount
) {

    public FoundryMetalLayer {
        if (metal == null) {
            throw new IllegalArgumentException(
                    "Foundry metal layer metal cannot be null."
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Foundry metal layer amount must be positive."
            );
        }
    }
}
