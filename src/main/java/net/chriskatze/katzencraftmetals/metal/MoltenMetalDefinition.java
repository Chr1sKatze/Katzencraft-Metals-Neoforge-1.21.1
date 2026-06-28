package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared non-storage information about one molten metal.
 *
 * Storage amounts remain integer molten units. The unitsPerOre value is the
 * canonical conversion used later by the Controller display:
 *
 *     ore equivalent = molten units / unitsPerOre
 *
 * density controls physical layer order. Higher density metals will be
 * placed below lower density metals.
 */
public record MoltenMetalDefinition(
        ResourceLocation id,
        String translationKey,
        ResourceLocation animatedTexture,
        int density,
        int unitsPerOre
) {

    public MoltenMetalDefinition {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Molten metal id cannot be null."
            );
        }

        if (
                translationKey == null
                        || translationKey.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Molten metal translation key cannot be blank."
            );
        }

        if (animatedTexture == null) {
            throw new IllegalArgumentException(
                    "Molten metal texture cannot be null."
            );
        }

        if (density <= 0) {
            throw new IllegalArgumentException(
                    "Molten metal density must be positive."
            );
        }

        if (unitsPerOre <= 0) {
            throw new IllegalArgumentException(
                    "Molten units per ore must be positive."
            );
        }
    }

    public double toOreAmount(
            int moltenUnits
    ) {
        return (double) moltenUnits
                / unitsPerOre;
    }
}
