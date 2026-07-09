package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.recipe.FoundryAlloyRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which molten metals this Controller has seen in its attached tank
 * network.
 *
 * This controls alloy recipe discovery/unlocking and keeps that bookkeeping
 * out of FoundryControllerBlockEntity.
 */
final class FoundryControllerMetalDiscovery {

    private static final String DISCOVERED_MOLTEN_METALS_TAG =
            "DiscoveredMoltenMetals";

    private final FoundryControllerBlockEntity controller;

    private final Set<ResourceLocation> discoveredMoltenMetals =
            new HashSet<>();

    FoundryControllerMetalDiscovery(
            FoundryControllerBlockEntity controller
    ) {
        this.controller =
                controller;
    }

    boolean hasDiscovered(
            ResourceLocation metal
    ) {
        return metal != null
                && discoveredMoltenMetals.contains(
                metal
        );
    }

    boolean isAlloyRecipeUnlocked(
            FoundryAlloyRecipe recipe
    ) {
        if (
                recipe == null
                        || recipe.ingredients()
                        .isEmpty()
        ) {
            return false;
        }

        for (var ingredient : recipe.ingredients()) {
            if (
                    !hasDiscovered(
                            ingredient.metal()
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    void discoverCurrentTankMetals() {
        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (network == null) {
            return;
        }

        network.ensureMoltenContentsMigrated();

        boolean changed =
                false;

        for (
                var entry :
                network.getMoltenContents()
                        .entrySet()
        ) {
            if (
                    entry.getValue() > 0
                            && ModMoltenMetals.contains(
                            entry.getKey()
                    )
            ) {
                changed |=
                        discoveredMoltenMetals.add(
                                entry.getKey()
                        );
            }
        }

        if (changed) {
            controller.setChanged();
        }
    }

    void save(
            CompoundTag tag
    ) {
        CompoundTag discoveredTag =
                new CompoundTag();

        for (
                ResourceLocation metal :
                discoveredMoltenMetals
        ) {
            discoveredTag.putBoolean(
                    metal.toString(),
                    true
            );
        }

        tag.put(
                DISCOVERED_MOLTEN_METALS_TAG,
                discoveredTag
        );
    }

    void load(
            CompoundTag tag
    ) {
        discoveredMoltenMetals.clear();

        if (
                !tag.contains(
                        DISCOVERED_MOLTEN_METALS_TAG,
                        Tag.TAG_COMPOUND
                )
        ) {
            return;
        }

        CompoundTag discoveredTag =
                tag.getCompound(
                        DISCOVERED_MOLTEN_METALS_TAG
                );

        for (String key : discoveredTag.getAllKeys()) {
            ResourceLocation metal =
                    ResourceLocation.tryParse(
                            key
                    );

            if (
                    metal != null
                            && discoveredTag.getBoolean(
                            key
                    )
                            && ModMoltenMetals.contains(
                            metal
                    )
            ) {
                discoveredMoltenMetals.add(
                        metal
                );
            }
        }
    }
}
