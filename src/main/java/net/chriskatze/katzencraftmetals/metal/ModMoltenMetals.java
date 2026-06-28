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

    /*
     * Copper is denser than Iron, so the future multi-metal distributor
     * will place Copper below Iron.
     *
     * Copper uses its own 20-frame animated texture sheet.
     */
    public static final MoltenMetalDefinition COPPER =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "copper"
                            ),
                            "metal.katzencraftmetals.copper",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_copper.png"
                            ),
                            8_960,
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

    /**
     * Compact integer id used only for menu synchronization.
     *
     * The order follows the insertion order of DEFINITIONS, which is stable
     * for one running mod version. Persistent saves continue to use the full
     * ResourceLocation and never depend on this number.
     */
    public static int getSyncId(
            ResourceLocation id
    ) {
        int index = 0;

        for (ResourceLocation registeredId : DEFINITIONS.keySet()) {
            if (registeredId.equals(id)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    public static Optional<MoltenMetalDefinition> bySyncId(
            int syncId
    ) {
        if (
                syncId < 0
                        || syncId >= DEFINITIONS.size()
        ) {
            return Optional.empty();
        }

        return DEFINITIONS.values()
                .stream()
                .skip(syncId)
                .findFirst();
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
