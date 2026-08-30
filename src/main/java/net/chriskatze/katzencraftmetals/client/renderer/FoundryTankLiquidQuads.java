package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;

/**
 * Low-level molten-liquid quad emission helpers.
 */
final class FoundryTankLiquidQuads {

    private FoundryTankLiquidQuads() {
    }

    static void renderLiquidSideSegment(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float minY,
            float maxY,
            boolean sharedBoundary,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (maxY - minY <= LIQUID_EPSILON) {
            return;
        }

        float coordinate =
                switch (side) {
                    case NORTH -> sharedBoundary
                            ? 0.0f
                            : geometry.minZ();
                    case SOUTH -> sharedBoundary
                            ? 1.0f
                            : geometry.maxZ();
                    case WEST -> sharedBoundary
                            ? 0.0f
                            : geometry.minX();
                    case EAST -> sharedBoundary
                            ? 1.0f
                            : geometry.maxX();
                    default -> throw new IllegalArgumentException(
                            "Liquid side must be horizontal."
                    );
                };

        /*
         * These side quads are intentionally wound so their front faces point
         * outward from the Tank.
         *
         * This matters because molten liquid now uses a culling translucent
         * render type. The old winding was effectively inside-facing for the
         * four side faces. With culling enabled, that made liquid sides visible
         * from inside the liquid and invisible from outside.
         *
         * Top and bottom faces were already wound correctly, so only the four
         * side directions are flipped here.
         */
        switch (side) {
            case NORTH -> renderLiquidQuad(
                    consumer,
                    pose,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    geometry.maxX(),
                    minY,
                    coordinate,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.minX(),
                    1.0f - maxY,
                    geometry.maxX(),
                    1.0f - maxY,
                    geometry.maxX(),
                    1.0f - minY,
                    geometry.minX(),
                    1.0f - minY,
                    0.0f,
                    0.0f,
                    -1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case SOUTH -> renderLiquidQuad(
                    consumer,
                    pose,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.maxX(),
                    minY,
                    coordinate,
                    1.0f - geometry.maxX(),
                    1.0f - maxY,
                    1.0f - geometry.minX(),
                    1.0f - maxY,
                    1.0f - geometry.minX(),
                    1.0f - minY,
                    1.0f - geometry.maxX(),
                    1.0f - minY,
                    0.0f,
                    0.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case WEST -> renderLiquidQuad(
                    consumer,
                    pose,
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    coordinate,
                    minY,
                    geometry.minZ(),
                    coordinate,
                    minY,
                    geometry.maxZ(),
                    1.0f - geometry.maxZ(),
                    1.0f - maxY,
                    1.0f - geometry.minZ(),
                    1.0f - maxY,
                    1.0f - geometry.minZ(),
                    1.0f - minY,
                    1.0f - geometry.maxZ(),
                    1.0f - minY,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case EAST -> renderLiquidQuad(
                    consumer,
                    pose,
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    coordinate,
                    minY,
                    geometry.maxZ(),
                    coordinate,
                    minY,
                    geometry.minZ(),
                    geometry.minZ(),
                    1.0f - maxY,
                    geometry.maxZ(),
                    1.0f - maxY,
                    geometry.maxZ(),
                    1.0f - minY,
                    geometry.minZ(),
                    1.0f - minY,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            default -> {
            }
        }
    }

    static void renderLiquidHorizontalFace(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (face == Direction.UP) {
            renderLiquidQuad(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    minX,
                    y,
                    maxZ,
                    maxX,
                    y,
                    maxZ,
                    maxX,
                    y,
                    minZ,
                    minX,
                    minZ,
                    minX,
                    maxZ,
                    maxX,
                    maxZ,
                    maxX,
                    minZ,
                    0.0f,
                    1.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        } else {
            renderLiquidQuad(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    minX,
                    y,
                    minZ,
                    maxX,
                    y,
                    minZ,
                    maxX,
                    y,
                    maxZ,
                    minX,
                    maxZ,
                    minX,
                    minZ,
                    maxX,
                    minZ,
                    maxX,
                    maxZ,
                    0.0f,
                    -1.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    static void renderLiquidQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float u1,
            float localV1,
            float u2,
            float localV2,
            float u3,
            float localV3,
            float u4,
            float localV4,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        addLiquidVertex(
                consumer,
                pose,
                x1,
                y1,
                z1,
                u1,
                localV1,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x2,
                y2,
                z2,
                u2,
                localV2,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x3,
                y3,
                z3,
                u3,
                localV3,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x4,
                y4,
                z4,
                u4,
                localV4,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );
    }

    private static void addLiquidVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float localV,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        float textureV =
                Mth.lerp(
                        localV,
                        frameMinV,
                        frameMaxV
                );

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(0xFFFFFFFF)
                .setUv(
                        u,
                        textureV
                )
                .setOverlay(packedOverlay)
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
