package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Set;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.*;

/** Encodes fixed per-cell edge metadata on the original four top vertices. */
final class FoundryTankSurfaceRenderer {
    private static final ResourceLocation IRON = ResourceLocation.parse("minecraft:iron");
    private static final ResourceLocation STEEL = ResourceLocation.parse("katzencraftmetals:steel");

    static int edgeMask(BlockPos pos, Set<BlockPos> cells) {
        boolean n = !cells.contains(pos.north()), s = !cells.contains(pos.south());
        boolean w = !cells.contains(pos.west()), e = !cells.contains(pos.east());
        int mask = (n ? 1 : 0) | (s ? 2 : 0) | (w ? 4 : 0) | (e ? 8 : 0);
        if (!n && !w && !cells.contains(pos.offset(-1, 0, -1))) mask |= 16;
        if (!n && !e && !cells.contains(pos.offset(1, 0, -1))) mask |= 32;
        if (!s && !w && !cells.contains(pos.offset(-1, 0, 1))) mask |= 64;
        if (!s && !e && !cells.contains(pos.offset(1, 0, 1))) mask |= 128;
        return mask;
    }

    static void render(MultiBufferSource buffers, VertexConsumer fallback, PoseStack.Pose pose,
                       FoundryTankLiquidGeometry g, BlockPos pos, Set<BlockPos> cells,
                       MoltenMetalDefinition metal, int surfaceCells, int overlay, float minV, float maxV) {
        RenderType type = FoundryTankSurfaceRenderType.get(metal.animatedTexture());
        if (type == null) {
            FoundryTankLiquidQuads.renderLiquidHorizontalFace(Direction.UP, fallback, pose,
                    g.minX(), g.maxX(), g.surfaceY(), g.minZ(), g.maxZ(), overlay, minV, maxV);
            return;
        }
        VertexConsumer out = buffers.getBuffer(type);
        int tint = FoundryMetalEffectPalette.baseTint(metal.id());
        float fade = Mth.lerp(Mth.clamp((LIQUID_MAX_INSET - g.surfaceY())
                / CONTACT_RIM_GLASS_FADE_DISTANCE, 0, 1), 0.60f, 1.0f);
        int alpha = Math.round(255 * FoundryMetalEffectPalette.rimMaxAlpha(metal.id()) * fade);
        // UV1 stores local X/Z. UV2 low bytes retain full light; high bytes carry
        // edge flags and the width-curve cell count. Shader masks signed shorts.
        int light = (240 | (edgeMask(pos, cells) << 8))
                | ((240 | (Math.min(surfaceCells, CONTACT_RIM_LARGE_SURFACE_CELLS) << 8)) << 16);
        emitQuad(out, pose, g, minV, maxV, tint, alpha, light);
    }

    /** Same side vertices and material, with depth below the exposed meniscus in UV1. */
    static VertexConsumer sideConsumer(MultiBufferSource buffers, VertexConsumer fallback,
                                       FoundryTankLiquidGeometry g, MoltenMetalDefinition metal,
                                       float minV, float maxV) {
        RenderType type = FoundryTankSurfaceRenderType.get(metal.animatedTexture());
        if (type == null) return fallback;
        float fade = Mth.lerp(Mth.clamp((LIQUID_MAX_INSET - g.surfaceY())
                / CONTACT_RIM_GLASS_FADE_DISTANCE, 0, 1), 0.60f, 1.0f);
        return new SideConsumer(buffers.getBuffer(type), g.surfaceY(),
                sideFadeDistance(metal.id(), g.surfaceY() - g.minY()), minV, maxV,
                FoundryMetalEffectPalette.baseTint(metal.id()),
                Math.round(255 * FoundryMetalEffectPalette.rimMaxAlpha(metal.id()) * fade));
    }

    static float sideFadeDistance(ResourceLocation metal, float layerHeight) {
        float maximum = metal.equals(IRON) || metal.equals(STEEL) ? 8.0f / 16.0f : 4.0f / 16.0f;
        // Shorten the fade proportionally in shallow layers, keeping bottom edges unlit.
        return Math.min(maximum, layerHeight * maximum);
    }

    private static final class SideConsumer implements VertexConsumer {
        private final VertexConsumer out;
        private final float top, fadeDistance, minV, maxV;
        private final int tint, alpha;
        private float depth;

        private SideConsumer(VertexConsumer out, float top, float fadeDistance,
                             float minV, float maxV, int tint, int alpha) {
            this.out = out;
            this.top = top;
            this.fadeDistance = fadeDistance;
            this.minV = minV;
            this.maxV = maxV;
            this.tint = tint;
            this.alpha = alpha;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            out.addVertex(x, y, z); return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            out.setColor((tint >> 16) & 255, (tint >> 8) & 255, tint & 255, alpha); return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            // Side UVs already encode local Y, including junction-clipped vertices.
            // Preserve them exactly for the animated liquid texture.
            depth = top - (1.0f - (v - minV) / (maxV - minV));
            out.setUv(u, v); return this;
        }
        @Override public VertexConsumer setUv1(int u, int v) {
            out.setUv1(Math.round(Mth.clamp(depth, 0, 1) * 32767),
                    Math.round(fadeDistance * 32767)); return this;
        }
        @Override public VertexConsumer setUv2(int u, int v) {
            // The high count bit selects side shading; low bytes retain original light.
            out.setUv2(u & 255, (v & 255) | (128 << 8)); return this;
        }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            out.setNormal(x, y, z); return this;
        }
    }

    static void emitQuad(VertexConsumer out, PoseStack.Pose pose, FoundryTankLiquidGeometry g,
                         float minV, float maxV, int tint, int alpha, int light) {
        vertex(out, pose, g.minX(), g.surfaceY(), g.minZ(), minV, maxV, tint, alpha, light);
        vertex(out, pose, g.minX(), g.surfaceY(), g.maxZ(), minV, maxV, tint, alpha, light);
        vertex(out, pose, g.maxX(), g.surfaceY(), g.maxZ(), minV, maxV, tint, alpha, light);
        vertex(out, pose, g.maxX(), g.surfaceY(), g.minZ(), minV, maxV, tint, alpha, light);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float minV, float maxV, int tint, int alpha, int light) {
        out.addVertex(pose.pose(), x, y, z)
                .setColor((tint >> 16) & 255, (tint >> 8) & 255, tint & 255, alpha)
                .setUv(x, Mth.lerp(z, minV, maxV))
                .setUv1(Math.round(x * 32767), Math.round(z * 32767)).setLight(light)
                .setNormal(pose, 0, 1, 0);
    }
}
