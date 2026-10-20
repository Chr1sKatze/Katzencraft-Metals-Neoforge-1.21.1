package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Catalog API uses integer ingot equivalents. The serializer continues to use molten units. */
public record MetalAlloy(List<Input> inputs, int processingTime) {
    public static final int UNITS_PER_INGOT = 6;
    public record Input(ResourceLocation metal, int ingots) {
        public Input {
            Objects.requireNonNull(metal, "Alloy input metal");
            if (ingots <= 0) throw new IllegalArgumentException("Alloy input must be positive whole ingots: " + metal);
            try { Math.multiplyExact(ingots, UNITS_PER_INGOT); }
            catch (ArithmeticException overflow) { throw new IllegalArgumentException("Alloy quantity overflows molten units: " + metal, overflow); }
        }
        public int moltenUnits() { return Math.multiplyExact(ingots, UNITS_PER_INGOT); }
    }
    public MetalAlloy {
        inputs = List.copyOf(inputs);
        if (inputs.isEmpty() || inputs.size() > 3 || processingTime <= 0)
            throw new IllegalArgumentException("Alloy requires 1–3 distinct inputs and positive processing time");
        var ids = new HashSet<ResourceLocation>();
        for (var input : inputs) if (!ids.add(input.metal()))
            throw new IllegalArgumentException("Duplicate alloy ingredient: " + input.metal());
    }
    /**
     * Whole ingot equivalents. Short iron/copper/gold names reference Minecraft;
     * other short names reference this mod. Explicit namespace:path IDs stay exact.
     * Catalog validation checks existence after all entries are built, allowing forward references.
     */
    public static Input input(String id, int ingots) {
        ResourceLocation metal = id.indexOf(':') >= 0 ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath(
                        switch (id) { case "iron", "copper", "gold" -> "minecraft"; default -> "katzencraftmetals"; }, id);
        return new Input(metal, ingots);
    }
}
