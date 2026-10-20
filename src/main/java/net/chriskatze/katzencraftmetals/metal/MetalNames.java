package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Common-side Components; no client localization classes or registry object access. */
public final class MetalNames {
    private MetalNames() {}
    public static Component name(MetalDefinition definition) {
        return Component.translatableWithFallback(definition.translationKey(), definition.englishName());
    }
    public static Component name(ResourceLocation id) {
        return MetalCatalog.get(id).map(MetalNames::name).orElseGet(() -> Component.literal(id.toString()));
    }
}
