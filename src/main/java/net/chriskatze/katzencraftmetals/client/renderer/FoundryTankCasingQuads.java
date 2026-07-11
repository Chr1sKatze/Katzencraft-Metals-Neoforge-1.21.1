package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;

/**
 * Low-level casing quad emission helpers.
 */
final class FoundryTankCasingQuads {

    private FoundryTankCasingQuads() {
    }

    static void renderSideRect(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minHorizontal,
            float maxHorizontal,
            float minY,
            float maxY,
            float minU,
            float maxU,
            float minV,
            float maxV,
            int packedLight,
            int packedOverlay,
            float outwardOffset
    ) {
        switch (face) {
            case NORTH -> renderCasingQuad(
                    consumer,
                    pose,
                    minHorizontal,
                    minY,
                    -outwardOffset,
                    maxHorizontal,
                    minY,
                    -outwardOffset,
                    maxHorizontal,
                    maxY,
                    -outwardOffset,
                    minHorizontal,
                    maxY,
                    -outwardOffset,
                    minU,
                    maxV,
                    maxU,
                    maxV,
                    maxU,
                    minV,
                    minU,
                    minV,
                    0.0f,
                    0.0f,
                    -1.0f,
                    packedLight,
                    packedOverlay
            );
            case SOUTH -> renderCasingQuad(
                    consumer,
                    pose,
                    1.0f - minHorizontal,
                    minY,
                    1.0f + outwardOffset,
                    1.0f - maxHorizontal,
                    minY,
                    1.0f + outwardOffset,
                    1.0f - maxHorizontal,
                    maxY,
                    1.0f + outwardOffset,
                    1.0f - minHorizontal,
                    maxY,
                    1.0f + outwardOffset,
                    minU,
                    maxV,
                    maxU,
                    maxV,
                    maxU,
                    minV,
                    minU,
                    minV,
                    0.0f,
                    0.0f,
                    1.0f,
                    packedLight,
                    packedOverlay
            );
            case WEST -> renderCasingQuad(
                    consumer,
                    pose,
                    -outwardOffset,
                    minY,
                    1.0f - minHorizontal,
                    -outwardOffset,
                    minY,
                    1.0f - maxHorizontal,
                    -outwardOffset,
                    maxY,
                    1.0f - maxHorizontal,
                    -outwardOffset,
                    maxY,
                    1.0f - minHorizontal,
                    minU,
                    maxV,
                    maxU,
                    maxV,
                    maxU,
                    minV,
                    minU,
                    minV,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedLight,
                    packedOverlay
            );
            case EAST -> renderCasingQuad(
                    consumer,
                    pose,
                    1.0f + outwardOffset,
                    minY,
                    minHorizontal,
                    1.0f + outwardOffset,
                    minY,
                    maxHorizontal,
                    1.0f + outwardOffset,
                    maxY,
                    maxHorizontal,
                    1.0f + outwardOffset,
                    maxY,
                    minHorizontal,
                    minU,
                    maxV,
                    maxU,
                    maxV,
                    maxU,
                    minV,
                    minU,
                    minV,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedLight,
                    packedOverlay
            );
            default -> throw new IllegalArgumentException(
                    "Tank side rectangle must use a horizontal face."
            );
        }
    }

    static void renderHorizontalRect(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float minU,
            float maxU,
            float minV,
            float maxV,
            int packedLight,
            int packedOverlay
    ) {
        float y =
                face == Direction.UP
                        ? 1.0f
                        : 0.0f;

        if (face == Direction.UP) {
            renderCasingQuad(
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
                    minU,
                    minV,
                    minU,
                    maxV,
                    maxU,
                    maxV,
                    maxU,
                    minV,
                    0.0f,
                    1.0f,
                    0.0f,
                    packedLight,
                    packedOverlay
            );
        } else {
            renderCasingQuad(
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
                    minU,
                    maxV,
                    minU,
                    minV,
                    maxU,
                    minV,
                    maxU,
                    maxV,
                    0.0f,
                    -1.0f,
                    0.0f,
                    packedLight,
                    packedOverlay
            );
        }
    }

    static void renderCasingQuad(
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
            float v1,
            float u2,
            float v2,
            float u3,
            float v3,
            float u4,
            float v4,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight,
            int packedOverlay
    ) {
        addCasingVertex(
                consumer,
                pose,
                x1,
                y1,
                z1,
                u1,
                v1,
                normalX,
                normalY,
                normalZ,
                packedLight,
                packedOverlay
        );

        addCasingVertex(
                consumer,
                pose,
                x2,
                y2,
                z2,
                u2,
                v2,
                normalX,
                normalY,
                normalZ,
                packedLight,
                packedOverlay
        );

        addCasingVertex(
                consumer,
                pose,
                x3,
                y3,
                z3,
                u3,
                v3,
                normalX,
                normalY,
                normalZ,
                packedLight,
                packedOverlay
        );

        addCasingVertex(
                consumer,
                pose,
                x4,
                y4,
                z4,
                u4,
                v4,
                normalX,
                normalY,
                normalZ,
                packedLight,
                packedOverlay
        );
    }

    private static void addCasingVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(0xFFFFFFFF)
                .setUv(
                        u,
                        v
                )
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
