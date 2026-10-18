package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Keeps near-coplanar tank walls in front without reopening repaired liquid boundaries. */
final class FoundryTankLiquidDepthRenderType extends RenderType {
    // Positive offset moves fragments away from the camera. No slope/world-space displacement.
    static final float DEPTH_FACTOR = 0.0f;
    static final float DEPTH_UNITS = 2.0f;
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    private FoundryTankLiquidDepthRenderType(RenderType base) {
        super("katzencraftmetals_tank_liquid_depth", base.format(), base.mode(), base.bufferSize(),
                base.affectsCrumbling(), base.sortOnUpload(),
                () -> {
                    base.setupRenderState();
                    GlStateManager._polygonOffset(DEPTH_FACTOR, DEPTH_UNITS);
                    GlStateManager._enablePolygonOffset();
                },
                () -> {
                    GlStateManager._polygonOffset(0.0f, 0.0f);
                    GlStateManager._disablePolygonOffset();
                    base.clearRenderState();
                });
    }

    static RenderType get(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, key -> wrap(RenderType.entityTranslucentCull(key)));
    }

    /** Blend types retain their own cache/reload lifetime and receive exactly the same depth treatment. */
    static RenderType wrap(RenderType base) {
        return new FoundryTankLiquidDepthRenderType(base);
    }
}
