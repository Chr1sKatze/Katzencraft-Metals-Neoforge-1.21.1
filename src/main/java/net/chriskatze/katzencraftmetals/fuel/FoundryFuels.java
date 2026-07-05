package net.chriskatze.katzencraftmetals.fuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Central Foundry fuel table.
 *
 * Step 10D intentionally uses vanilla items so the full temperature ladder can
 * be tested without adding temporary item textures. These items can later be
 * replaced by dedicated Foundry fuels without changing the Controller logic.
 */
public final class FoundryFuels {

    public static final FoundryFuelDefinition CHARCOAL =
            new FoundryFuelDefinition(
                    Items.CHARCOAL,
                    1_600,
                    850
            );

    public static final FoundryFuelDefinition COAL =
            new FoundryFuelDefinition(
                    Items.COAL,
                    1_600,
                    900
            );

    public static final FoundryFuelDefinition BLAZE_POWDER =
            new FoundryFuelDefinition(
                    Items.BLAZE_POWDER,
                    2_400,
                    1_300
            );

    public static final FoundryFuelDefinition BLAZE_ROD =
            new FoundryFuelDefinition(
                    Items.BLAZE_ROD,
                    3_200,
                    1_700
            );

    public static final FoundryFuelDefinition NETHER_STAR =
            new FoundryFuelDefinition(
                    Items.NETHER_STAR,
                    8_000,
                    2_200
            );

    private static final List<FoundryFuelDefinition> DEFINITIONS =
            List.of(
                    CHARCOAL,
                    COAL,
                    BLAZE_POWDER,
                    BLAZE_ROD,
                    NETHER_STAR
            );

    private FoundryFuels() {
    }

    public static Optional<FoundryFuelDefinition> find(
            ItemStack stack
    ) {
        return DEFINITIONS.stream()
                .filter(
                        definition ->
                                definition.matches(stack)
                )
                .findFirst();
    }

    public static boolean isFuel(
            ItemStack stack
    ) {
        return find(stack).isPresent();
    }

    /**
     * Chooses the weakest available fuel that can satisfy the requested
     * temperature. This prevents high-tier fuel from being wasted on Copper or
     * Iron when Coal is also present.
     */
    public static Optional<FoundryFuelDefinition> weakestCapable(
            List<ItemStack> stacks,
            int requiredTemperature
    ) {
        return stacks.stream()
                .map(FoundryFuels::find)
                .flatMap(Optional::stream)
                .filter(
                        definition ->
                                definition.maximumTemperature()
                                        >= requiredTemperature
                )
                .min(
                        Comparator
                                .comparingInt(
                                        FoundryFuelDefinition
                                                ::maximumTemperature
                                )
                                .thenComparingInt(
                                        FoundryFuelDefinition
                                                ::burnTime
                                )
                );
    }

    public static List<FoundryFuelDefinition> values() {
        return DEFINITIONS;
    }
}
