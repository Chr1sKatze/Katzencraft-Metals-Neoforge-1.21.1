package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.Set;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.*;

/** Encodes fixed per-cell edge metadata on the original four top vertices. */
final class FoundryTankSurfaceRenderer {
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
