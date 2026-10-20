package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;

/** Null overrides reuse the conventional molten image for still/flowing and the storage block for cooled casts. */
public record MetalFoundrySupport(ResourceLocation still, ResourceLocation flowing,
                                  ResourceLocation cooled, boolean optionalFlowing) {
    public static MetalFoundrySupport standard() { return new MetalFoundrySupport(null, null, null, false); }
    public MetalFoundrySupport still(ResourceLocation texture) {
        return new MetalFoundrySupport(java.util.Objects.requireNonNull(texture), flowing, cooled, optionalFlowing);
    }
    public MetalFoundrySupport flowing(ResourceLocation texture) {
        return new MetalFoundrySupport(still, java.util.Objects.requireNonNull(texture), cooled, false);
    }
    public MetalFoundrySupport cooled(ResourceLocation texture) {
        return new MetalFoundrySupport(still, flowing, java.util.Objects.requireNonNull(texture), optionalFlowing);
    }
    public static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("katzencraftmetals", "textures/block/" + path + ".png");
    }
}
