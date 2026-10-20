package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.Optional;

/** A view of the existing catalog, not a registry or a second list of metals. */
public final class StandardMetalForms {
    public record Entry(MetalDefinition metal, ResourceLocation id, MetalDefinition.Form form) {
        public Item item() { return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(); }
        public Block block() { return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(); }
    }
    private StandardMetalForms() {}
    private static final List<Entry> ENTRIES = collect();
    private static final java.util.Map<ResourceLocation, Entry> BY_ID = ENTRIES.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Entry::id, entry -> entry));
    public static List<Entry> entries() { return ENTRIES; }
    private static List<Entry> collect() {
        return MetalCatalog.values().stream()
                .flatMap(metal -> metal.forms().entrySet().stream()
                        .filter(form -> form.getKey().getNamespace().equals("katzencraftmetals"))
                        .filter(form -> form.getValue() == MetalDefinition.Form.INGOT || form.getValue() == MetalDefinition.Form.NUGGET
                                || form.getValue() == MetalDefinition.Form.STORAGE_BLOCK)
                        .map(form -> new Entry(metal, form.getKey(), form.getValue()))).toList();
    }
    public static Optional<Entry> find(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
