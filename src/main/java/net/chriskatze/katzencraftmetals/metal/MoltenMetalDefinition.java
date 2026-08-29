package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Shared non-storage information about one molten metal.
 *
 * Storage amounts remain integer molten units. The unitsPerOre value is the
 * canonical conversion used by the Controller display:
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
        ResourceLocation flowingTexture,
        ResourceLocation cooledTexture,
        int density,
        int unitsPerOre,
        Supplier<ItemStack> castResultFactory
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
                    "Molten metal animated texture cannot be null."
            );
        }

        if (flowingTexture == null) {
            throw new IllegalArgumentException(
                    "Molten metal flowing texture cannot be null."
            );
        }

        if (cooledTexture == null) {
            throw new IllegalArgumentException(
                    "Molten metal cooled texture cannot be null."
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

        if (castResultFactory == null) {
            throw new IllegalArgumentException(
                    "Molten metal cast result factory cannot be null."
            );
        }
    }

    public double toOreAmount(
            int moltenUnits
    ) {
        return (double) moltenUnits
                / unitsPerOre;
    }

    public ItemStack createCastResult() {
        ItemStack result =
                castResultFactory.get();

        return result == null
                ? ItemStack.EMPTY
                : result.copy();
    }
}
