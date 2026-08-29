package net.chriskatze.katzencraftmetals.metal;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Central list of molten metals understood by the Foundry. */
public final class ModMoltenMetals {

    private static final Map<ResourceLocation, MoltenMetalDefinition>
            DEFINITIONS =
            new LinkedHashMap<>();

    public static final MoltenMetalDefinition IRON =
            register(
                    FoundryMetalBuilder.vanilla(
                                    "iron"
                            )
                            .animatedTexture(
                                    "molten_iron"
                            )
                            .cooledTexture(
                                    "cooled_iron"
                            )
                            .density(
                                    7_874
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    Items.IRON_BLOCK
                            )
                            .build()
            );

    public static final MoltenMetalDefinition COPPER =
            register(
                    FoundryMetalBuilder.vanilla(
                                    "copper"
                            )
                            .animatedTexture(
                                    "molten_copper"
                            )
                            .vanillaCooledTexture(
                                    "copper_block"
                            )
                            .density(
                                    8_960
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    Items.COPPER_BLOCK
                            )
                            .build()
            );

    public static final MoltenMetalDefinition STEEL =
            register(
                    FoundryMetalBuilder.mod(
                                    "steel"
                            )
                            .animatedTexture(
                                    "molten_steel"
                            )
                            .flowingTexture(
                                    "molten_steel_flowing"
                            )
                            .cooledTexture(
                                    "steel_block"
                            )
                            .density(
                                    7_850
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    ModBlocks.STEEL_BLOCK.get()
                            )
                            .build()
            );

    public static final MoltenMetalDefinition MYTHRIL =
            register(
                    FoundryMetalBuilder.mod(
                                    "mythril"
                            )
                            .animatedTexture(
                                    "molten_mythril"
                            )
                            .flowingTexture(
                                    "molten_mythril_flowing"
                            )
                            .cooledTexture(
                                    "mythril_block"
                            )
                            .density(
                                    9_500
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    ModBlocks.MYTHRIL_BLOCK.get()
                            )
                            .build()
            );

    public static final MoltenMetalDefinition GOLD =
            register(
                    FoundryMetalBuilder.vanilla(
                                    "gold"
                            )
                            .animatedTexture(
                                    "molten_gold"
                            )
                            .flowingTexture(
                                    "molten_gold_flowing"
                            )
                            .vanillaCooledTexture(
                                    "gold_block"
                            )
                            .density(
                                    19_300
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    Items.GOLD_BLOCK
                            )
                            .build()
            );

    public static final MoltenMetalDefinition PLATINUM =
            register(
                    FoundryMetalBuilder.mod(
                                    "platinum"
                            )
                            .animatedTexture(
                                    "molten_platinum"
                            )
                            .flowingTexture(
                                    "molten_platinum_flowing"
                            )
                            .cooledTexture(
                                    "platinum_block"
                            )
                            .density(
                                    21_450
                            )
                            .unitsPerOre(
                                    6
                            )
                            .castResult(
                                    ModBlocks.PLATINUM_BLOCK.get()
                            )
                            .build()
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
