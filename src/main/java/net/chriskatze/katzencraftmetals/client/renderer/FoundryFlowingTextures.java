package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.metal.FoundryMetalBuilder;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.chriskatze.katzencraftmetals.metal.MetalDefinition;
import net.chriskatze.katzencraftmetals.metal.MetalCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.HashMap;
import java.util.Map;

/** Resolves optional flowing textures once per resource reload; explicit legacy mappings are preserved. */
public final class FoundryFlowingTextures extends SimplePreparableReloadListener<Map<ResourceLocation, ResourceLocation>> {
    private static volatile Map<ResourceLocation, ResourceLocation> textures = Map.of();

    static ResourceLocation get(MoltenMetalDefinition metal) {
        return textures.getOrDefault(metal.id(), optional(metal.id()) ? metal.animatedTexture() : metal.flowingTexture());
    }

    private static boolean optional(ResourceLocation id) {
        return MetalCatalog.get(id)
                .map(metal -> metal.foundry() != null && metal.foundry().optionalFlowing()).orElse(false);
    }

    static ResourceLocation resolve(ResourceManager resources, MetalDefinition definition) {
        var metal = FoundryMetalBuilder.fromCatalog(definition);
        return !definition.foundry().optionalFlowing() || resources.getResource(metal.flowingTexture()).isPresent()
                ? metal.flowingTexture() : metal.animatedTexture();
    }

    @Override
    protected Map<ResourceLocation, ResourceLocation> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, ResourceLocation> resolved = new HashMap<>();
        for (var metal : MetalCatalog.values()) {
            if (metal.foundry() != null) resolved.put(metal.id(), resolve(resources, metal));
        }
        return Map.copyOf(resolved);
    }

    @Override
    protected void apply(Map<ResourceLocation, ResourceLocation> resolved, ResourceManager resources, ProfilerFiller profiler) {
        textures = Map.copyOf(resolved);
    }
}
