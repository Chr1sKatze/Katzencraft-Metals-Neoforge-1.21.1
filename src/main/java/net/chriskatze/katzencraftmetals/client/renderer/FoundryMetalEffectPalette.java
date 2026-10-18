package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Client-only, resource-pack-aware base colors; effect alpha/intensity stay at their call sites. */
public final class FoundryMetalEffectPalette extends SimplePreparableReloadListener<Map<ResourceLocation, Integer>> {
    private static final ResourceLocation IRON = ResourceLocation.parse("minecraft:iron");
    private static final ResourceLocation STEEL = ResourceLocation.parse("katzencraftmetals:steel");
    private static final Map<ResourceLocation, Integer> CURATED = Map.of(
            STEEL, 0xF28E58,
            IRON, 0xFFD080,
            ResourceLocation.parse("minecraft:copper"), 0xFFE3BC,
            ResourceLocation.parse("minecraft:gold"), 0xFFF0CC,
            ResourceLocation.parse("katzencraftmetals:mythril"), 0xFFE9D0,
            ResourceLocation.parse("katzencraftmetals:platinum"), 0xFFE5D3
    );
    private static volatile Map<ResourceLocation, Integer> automatic = Map.of();

    static int baseTint(ResourceLocation metal) {
        Integer override = CURATED.get(metal);
        return override != null ? override : automatic.getOrDefault(metal, MoltenTextureColor.FALLBACK);
    }

    static float rimMaxAlpha(ResourceLocation metal) {
        return CURATED.containsKey(metal) && !metal.equals(STEEL) && !metal.equals(IRON) ? .30f
                : FoundryTankRenderConstants.CONTACT_RIM_MAX_ALPHA;
    }

    @Override
    protected Map<ResourceLocation, Integer> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, MoltenTextureColor.Profile> profiles = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> textureByMetal = new HashMap<>();
        for (var metal : ModMoltenMetals.values()) {
            textureByMetal.put(metal.id(), metal.animatedTexture());
            if (!profiles.containsKey(metal.animatedTexture())) {
                profiles.put(metal.animatedTexture(), readProfile(resources, metal.animatedTexture()));
            }
        }
        return derivePalette(profiles, textureByMetal);
    }

    static Map<ResourceLocation, Integer> derivePalette(
            Map<ResourceLocation, MoltenTextureColor.Profile> profiles,
            Map<ResourceLocation, ResourceLocation> textureByMetal) {
        ResourceLocation ironTexture = textureByMetal.get(IRON), steelTexture = textureByMetal.get(STEEL);
        var iron = new MoltenTextureColor.Reference(ironTexture == null ? null : profiles.get(ironTexture), CURATED.get(IRON));
        var steel = new MoltenTextureColor.Reference(steelTexture == null ? null : profiles.get(steelTexture), CURATED.get(STEEL));
        Map<ResourceLocation, Integer> result = new HashMap<>();
        textureByMetal.forEach((metal, texture) -> result.put(metal,
                MoltenTextureColor.derive(profiles.get(texture), iron, steel)));
        return Map.copyOf(result);
    }

    private static MoltenTextureColor.Profile readProfile(ResourceManager resources, ResourceLocation texture) {
        try (var stream = resources.open(texture); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream)) {
            long pixels = (long) image.getWidth() * image.getHeight();
            int step = Math.max(1, (int) Math.ceil(pixels / 262144.0));
            int[] sample = new int[(int) ((pixels + step - 1) / step)];
            for (int i = 0; i < sample.length; i++) {
                long index = (long) i * step;
                int abgr = image.getPixelRGBA((int) (index % image.getWidth()), (int) (index / image.getWidth()));
                sample[i] = (abgr & 0xFF00FF00) | ((abgr & 255) << 16) | ((abgr >>> 16) & 255);
            }
            return MoltenTextureColor.analyze(sample);
        } catch (IOException | RuntimeException error) {
            KatzencraftMetalsMod.LOGGER.warn("Could not derive molten effect color from {}; using fallback", texture, error);
            return null;
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, Integer> colors, ResourceManager resources, ProfilerFiller profiler) {
        // Publish one immutable snapshot; removed/replaced resources cannot leave stale entries.
        automatic = Map.copyOf(colors);
    }

}
