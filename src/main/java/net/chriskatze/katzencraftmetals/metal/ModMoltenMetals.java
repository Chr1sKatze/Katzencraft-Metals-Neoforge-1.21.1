package net.chriskatze.katzencraftmetals.metal;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/cooled_iron.png"
                            ),
                            7_874,
                            6,
                            () -> new ItemStack(
                                    Items.IRON_BLOCK
                            )
                    )
            );

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
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "textures/block/copper_block.png"
                            ),
                            8_960,
                            6,
                            () -> new ItemStack(
                                    Items.COPPER_BLOCK
                            )
                    )
            );

    /*
     * Temporary Step 10C alloy outputs. Dedicated molten textures can replace
     * these reused animations later without changing storage or recipes.
     */
    public static final MoltenMetalDefinition STEEL =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "steel"
                            ),
                            "metal.katzencraftmetals.steel",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_iron.png"
                            ),
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/steel_block.png"
                            ),
                            7_850,
                            6,
                            () -> new ItemStack(
                                    ModBlocks.STEEL_BLOCK.get()
                            )
                    )
            );

    public static final MoltenMetalDefinition MYTHRIL =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "mythril"
                            ),
                            "metal.katzencraftmetals.mythril",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_copper.png"
                            ),
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/mythril_block.png"
                            ),
                            9_500,
                            6,
                            () -> new ItemStack(
                                    ModBlocks.MYTHRIL_BLOCK.get()
                            )
                    )
            );

    public static final MoltenMetalDefinition GOLD =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "gold"
                            ),
                            "metal.katzencraftmetals.gold",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_copper.png"
                            ),
                            ResourceLocation.fromNamespaceAndPath(
                                    "minecraft",
                                    "textures/block/gold_block.png"
                            ),
                            19_300,
                            6,
                            () -> new ItemStack(
                                    Items.GOLD_BLOCK
                            )
                    )
            );

    public static final MoltenMetalDefinition PLATINUM =
            register(
                    new MoltenMetalDefinition(
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "platinum"
                            ),
                            "metal.katzencraftmetals.platinum",
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/molten_copper.png"
                            ),
                            ResourceLocation.fromNamespaceAndPath(
                                    KatzencraftMetalsMod.MODID,
                                    "textures/block/platinum_block.png"
                            ),
                            21_450,
                            6,
                            () -> new ItemStack(
                                    ModBlocks.PLATINUM_BLOCK.get()
                            )
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
