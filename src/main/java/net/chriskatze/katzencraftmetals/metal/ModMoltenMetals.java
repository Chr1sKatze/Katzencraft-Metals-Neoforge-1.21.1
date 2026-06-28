package net.chriskatze.katzencraftmetals.metal;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central list of molten metals understood by the Foundry.
 *
 * This is intentionally a small Java registry rather than a NeoForge
 * DeferredRegister. The entries describe Foundry behavior and visuals; they
 * are not standalone Minecraft registry objects.
 */
public final class ModMoltenMetals {

    private static final Map<ResourceLocation, MoltenMetalDefinition>
            DEFINITIONS =
            new LinkedHashMap<>();

    /*
     * Keep minecraft:iron as the id because that is already the id stored
     * by the existing Tank saves.
     *
     * Density is represented in kg/m³ only to provide a clear, stable
     * ordering. The exact number is not used for fluid simulation.
     */
    public static final MoltenMetalDefinition IRON =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "iron"
                            ),
                            "metal.katzencraftmetals.iron",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_iron.png"
                            ),
                            7_874,
                            6
                    )
            );

    private ModMoltenMetals() {
    }

    private static MoltenMetalDefinition register(
            MoltenMetalDefinition definition
    ) {
        MoltenMetalDefinition previous =
                DEFINITIONS.putIfAbsent(
                        definition.id(),
                        definition
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate molten metal definition: "
                            + definition.id()
            );
        }

        return definition;
    }

    public static Optional<MoltenMetalDefinition> get(
            ResourceLocation id
    ) {
        return Optional.ofNullable(
                DEFINITIONS.get(id)
        );
    }

    public static boolean contains(
            ResourceLocation id
    ) {
        return DEFINITIONS.containsKey(id);
    }

    public static Collection<MoltenMetalDefinition> values() {
        return Collections.unmodifiableCollection(
                DEFINITIONS.values()
        );
    }

    /**
     * Lightest first, matching how the Controller list will be displayed.
     */
    public static List<MoltenMetalDefinition> lightestFirst() {
        return DEFINITIONS.values()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                MoltenMetalDefinition::density
                        )
                )
                .toList();
    }

    /**
     * Heaviest first, matching bottom-up Tank layer construction.
     */
    public static List<MoltenMetalDefinition> heaviestFirst() {
        return DEFINITIONS.values()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                        MoltenMetalDefinition::density
                                )
                                .reversed()
                )
                .toList();
    }
}
