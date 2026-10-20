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

    /** One origin per contiguous liquid column; empty cells never relay surface heat. */
    record SideGlow(float top, float distance, float glassFade) {}

    static SideGlow sideGlow(BlockPos pos, java.util.Map<BlockPos, java.util.List<FoundryTankRenderedMetalLayer>> layers) {
        BlockPos surface = pos;
        var column = layers.getOrDefault(surface, java.util.List.of());
        if (column.isEmpty()) return null;
        while (column.getLast().maxY() >= 1.0f - LIQUID_EPSILON) {
            var above = layers.getOrDefault(surface.above(), java.util.List.of());
            if (above.isEmpty() || above.getFirst().minY() > LIQUID_EPSILON) break;
            // The longest band is half a block; a surface two cells above cannot contribute.
            if (surface.getY() - pos.getY() >= 1) return null;
            surface = surface.above();
            column = above;
        }
        var top = column.getLast();
        if (!top.renderTop()) return null;
        float origin = surface.getY() - pos.getY() + top.maxY();
        float distance = sideFadeDistance(top.metal(), 1);
        float fade = Mth.lerp(Mth.clamp((LIQUID_MAX_INSET - top.maxY())
                / CONTACT_RIM_GLASS_FADE_DISTANCE, 0, 1), 0.60f, 1.0f);
        return new SideGlow(origin, distance, fade);
    }

    static VertexConsumer sideConsumer(MultiBufferSource buffers, VertexConsumer fallback,
                                       FoundryTankLiquidGeometry g, MoltenMetalDefinition metal,
                                       float minV, float maxV) {
        return sideConsumer(buffers, fallback, g, metal, minV, maxV,
                new SideGlow(g.surfaceY(), sideFadeDistance(metal.id(), 1),
                        Mth.lerp(Mth.clamp((LIQUID_MAX_INSET - g.surfaceY())
                                / CONTACT_RIM_GLASS_FADE_DISTANCE, 0, 1), 0.60f, 1.0f)));
    }

    static VertexConsumer sideConsumer(MultiBufferSource buffers, VertexConsumer fallback,
                                       FoundryTankLiquidGeometry g, MoltenMetalDefinition metal,
                                       float minV, float maxV, SideGlow glow) {
        if (glow == null || glow.top() - g.surfaceY() >= glow.distance()) return fallback;
        RenderType type = FoundryTankSurfaceRenderType.get(metal.animatedTexture());
        if (type == null) return fallback;
        return new SideConsumer(buffers.getBuffer(type), glow, minV, maxV, metal, null);
    }

    static VertexConsumer interfaceConsumer(VertexConsumer out, SideGlow glow,
                                            MoltenMetalDefinition lower, MoltenMetalDefinition upper,
                                            float minV, float maxV) {
        return glow == null ? out : new SideConsumer(out, glow, minV, maxV, lower, upper);
    }

    static float sideFadeDistance(ResourceLocation metal, float ignoredLayerHeight) {
        return metal.equals(IRON) || metal.equals(STEEL) ? 8.0f / 16.0f : 4.0f / 16.0f;
    }

    private static final class SideConsumer implements VertexConsumer {
        private final VertexConsumer out;
        private final float top, fadeDistance, minV, maxV;
        private final int tint, alpha, upperTint, upperAlpha;
        private final boolean interfaceBand;
        private float depth;

        private SideConsumer(VertexConsumer out, SideGlow glow, float minV, float maxV,
                             MoltenMetalDefinition metal, MoltenMetalDefinition upper) {
            this.out = out;
            this.top = glow.top();
            this.fadeDistance = glow.distance();
            this.minV = minV;
            this.maxV = maxV;
            this.tint = FoundryMetalEffectPalette.baseTint(metal.id());
            this.alpha = Math.round(255 * FoundryMetalEffectPalette.rimMaxAlpha(metal.id()) * glow.glassFade());
            this.interfaceBand = upper != null;
            this.upperTint = upper == null ? tint : FoundryMetalEffectPalette.baseTint(upper.id());
            this.upperAlpha = upper == null ? alpha : Math.round(255 * FoundryMetalEffectPalette.rimMaxAlpha(upper.id()) * glow.glassFade());
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
            if (interfaceBand) {
                float t = Mth.clamp((u / 32767.0f * 4.0f - 1.0f) / 2.0f, 0, 1);
                t = t * t * (3 - 2 * t);
                out.setColor(Math.round(Mth.lerp(t, (tint >> 16) & 255, (upperTint >> 16) & 255)),
                        Math.round(Mth.lerp(t, (tint >> 8) & 255, (upperTint >> 8) & 255)),
                        Math.round(Mth.lerp(t, tint & 255, upperTint & 255)), Math.round(Mth.lerp(t, alpha, upperAlpha)));
                out.setUv1(u, Math.round(Mth.clamp(depth / fadeDistance / 8, 0, 1) * 32767));
            } else {
                // Four-block encoding range avoids clamping before interpolation across a cell.
                out.setUv1(Math.round(Mth.clamp(depth / 4, 0, 1) * 32767),
                        Math.round(fadeDistance * 32767));
            }
            return this;
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
