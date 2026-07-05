package net.chriskatze.katzencraftmetals.fuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/** Central Foundry fuel table. */
public final class FoundryFuels {

    public static final FoundryFuelDefinition CHARCOAL =
            new FoundryFuelDefinition(
                    Items.CHARCOAL,
                    1_600
            );

    public static final FoundryFuelDefinition COAL =
            new FoundryFuelDefinition(
                    Items.COAL,
                    1_600
            );

    public static final FoundryFuelDefinition BLAZE_POWDER =
            new FoundryFuelDefinition(
                    Items.BLAZE_POWDER,
                    2_400
            );

    public static final FoundryFuelDefinition BLAZE_ROD =
            new FoundryFuelDefinition(
                    Items.BLAZE_ROD,
                    3_200
            );

    public static final FoundryFuelDefinition NETHER_STAR =
            new FoundryFuelDefinition(
                    Items.NETHER_STAR,
                    8_000
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

    public static List<FoundryFuelDefinition> values() {
        return DEFINITIONS;
    }
}
