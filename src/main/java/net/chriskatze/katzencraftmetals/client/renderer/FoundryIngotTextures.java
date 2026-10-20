package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.HashMap;
import java.util.Map;

/** Resolves optional, hand-painted casting textures once per resource reload. */
public final class FoundryIngotTextures extends SimplePreparableReloadListener<Map<ResourceLocation, ResourceLocation>> {
    static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID,
            "textures/block/foundry_table_ingot_base.png");
    private static volatile Map<ResourceLocation, ResourceLocation> textures = Map.of();

    static ResourceLocation get(ResourceLocation metal) {
        return textures.getOrDefault(metal, BASE);
    }

    static ResourceLocation resolve(ResourceManager resources, ResourceLocation metal) {
        ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID,
                "textures/block/foundry_table_ingot_" + metal.getPath() + ".png");
        return resources.getResource(candidate).isPresent() ? candidate : BASE;
    }

    @Override
    protected Map<ResourceLocation, ResourceLocation> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, ResourceLocation> resolved = new HashMap<>();
        for (var metal : ModMoltenMetals.values()) resolved.put(metal.id(), resolve(resources, metal.id()));
        return Map.copyOf(resolved);
    }

    @Override
    protected void apply(Map<ResourceLocation, ResourceLocation> resolved, ResourceManager resources, ProfilerFiller profiler) {
        textures = Map.copyOf(resolved);
    }
}
