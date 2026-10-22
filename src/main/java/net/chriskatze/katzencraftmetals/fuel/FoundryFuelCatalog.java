package net.chriskatze.katzencraftmetals.fuel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/**
 * The single configuration point for Foundry fuels. Append fuel("namespace:item", ticks)
 * after registering that item normally; no machine, GUI or furnace changes are needed.
 * Durations are real burn ticks, independent of metal melting times and processing speed.
 * Active saved burns retain their stored duration when catalog values change.
 */
public final class FoundryFuelCatalog {
    private static final List<FoundryFuelDefinition> DEFINITIONS = List.of(
            fuel("minecraft:coal", 800),
            fuel("minecraft:charcoal", 800),
            fuel("minecraft:blaze_rod", 1_200)
    );
    private static final Map<ResourceLocation, FoundryFuelDefinition> BY_ITEM = index(DEFINITIONS);
    private FoundryFuelCatalog() {}

    public static FoundryFuelDefinition fuel(String item, int burnTicks) {
        return new FoundryFuelDefinition(ResourceLocation.parse(item), burnTicks);
    }
    public static List<FoundryFuelDefinition> values() { return DEFINITIONS; }
    public static Optional<FoundryFuelDefinition> find(ItemStack stack) {
        return stack == null || stack.isEmpty() ? Optional.empty()
                : Optional.ofNullable(BY_ITEM.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }
    public static boolean isFuel(ItemStack stack) { return find(stack).isPresent(); }
    /** Fail clearly on duplicate definitions rather than silently selecting one. */
    public static Map<ResourceLocation, FoundryFuelDefinition> index(List<FoundryFuelDefinition> definitions) {
        var result = new LinkedHashMap<ResourceLocation, FoundryFuelDefinition>();
        for (var definition : definitions) if (result.putIfAbsent(definition.item(), definition) != null)
            throw new IllegalArgumentException("Duplicate Foundry fuel: " + definition.item());
        return Collections.unmodifiableMap(result);
    }
}
