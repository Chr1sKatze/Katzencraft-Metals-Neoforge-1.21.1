package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Central list of molten metals understood by the Foundry. */
public final class ModMoltenMetals {

    private static final Map<ResourceLocation, MoltenMetalDefinition> DEFINITIONS = createDefinitions();
    // Definitions/densities are immutable. Preserve stable catalog order for density ties.
    private static final List<MoltenMetalDefinition> LIGHTEST_FIRST = DEFINITIONS.values().stream()
            .sorted(Comparator.comparingInt(MoltenMetalDefinition::density)).toList();
    private static final List<MoltenMetalDefinition> HEAVIEST_FIRST = DEFINITIONS.values().stream()
            .sorted(Comparator.comparingInt(MoltenMetalDefinition::density).reversed()).toList();

    private static Map<ResourceLocation, MoltenMetalDefinition> createDefinitions() {
        Map<ResourceLocation, MoltenMetalDefinition> definitions = new LinkedHashMap<>();
        for (var metal : MetalCatalog.values()) {
            if (metal.foundry() != null) definitions.put(metal.id(), FoundryMetalBuilder.fromCatalog(metal));
        }
        return Collections.unmodifiableMap(definitions);
    }

    public static final MoltenMetalDefinition IRON = DEFINITIONS.get(ResourceLocation.parse("minecraft:iron"));
    public static final MoltenMetalDefinition COPPER = DEFINITIONS.get(ResourceLocation.parse("minecraft:copper"));
    public static final MoltenMetalDefinition STEEL = DEFINITIONS.get(ResourceLocation.parse("katzencraftmetals:steel"));
    public static final MoltenMetalDefinition MYTHRIL = DEFINITIONS.get(ResourceLocation.parse("katzencraftmetals:mythril"));
    public static final MoltenMetalDefinition GOLD = DEFINITIONS.get(ResourceLocation.parse("minecraft:gold"));
    public static final MoltenMetalDefinition PLATINUM = DEFINITIONS.get(ResourceLocation.parse("katzencraftmetals:platinum"));

    private ModMoltenMetals() {}

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
        return LIGHTEST_FIRST;
    }

    public static List<MoltenMetalDefinition> heaviestFirst() {
        return HEAVIEST_FIRST;
    }
}
