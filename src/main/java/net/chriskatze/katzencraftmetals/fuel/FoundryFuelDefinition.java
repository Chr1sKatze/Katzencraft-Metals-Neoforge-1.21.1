package net.chriskatze.katzencraftmetals.fuel;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** Registry-independent Foundry fuel metadata; durations are never furnace fuel values. */
public record FoundryFuelDefinition(ResourceLocation item, int burnTime) {
    public FoundryFuelDefinition {
        Objects.requireNonNull(item, "Foundry fuel item ID");
        if (item.equals(ResourceLocation.withDefaultNamespace("air")) || burnTime <= 0)
            throw new IllegalArgumentException("Foundry fuel requires a non-air item and positive burn duration");
    }
}
