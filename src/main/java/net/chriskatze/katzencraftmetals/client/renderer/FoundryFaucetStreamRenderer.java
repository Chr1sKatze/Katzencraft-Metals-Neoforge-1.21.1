package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetRenderConstants.*;

final class FoundryFaucetStreamRenderer {

    private FoundryFaucetStreamRenderer() {
    }

    static void render(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryFaucetStreamGeometry geometry,
            FoundryFaucetDripStyle dripStyle,
            float dripDisappearY,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        if (
                geometry.channelInnerZ()
                        - geometry.channelOuterZ()
                        > 0.0001f
        ) {
            renderChannelLiquid(
                    consumer,
                    pose,
                    geometry.channelOuterZ(),
                    geometry.channelInnerZ(),
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }

        if (geometry.renderBrokenDrips()) {
            renderBrokenDrips(
                    consumer,
                    pose,
                    geometry.dripFieldBottomY(),
                    geometry.dripFieldTopY(),
                    dripDisappearY,
                    geometry.dripBreakupProgress(),
                    dripStyle,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        } else if (geometry.streamBottomY() < geometry.streamOuterTopY()) {
            renderStream(
                    consumer,
                    pose,
                    geometry.streamBottomY(),
                    geometry.streamInnerTopY(),
                    geometry.streamOuterTopY(),
                    1.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }
    }

    private static void renderBrokenDrips(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fieldBottomY,
            float fieldTopY,
            float disappearY,
            float breakupProgress,
            FoundryFaucetDripStyle dripStyle,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float fieldHeight =
                fieldTopY
                        - fieldBottomY;

        if (fieldHeight <= 0.0001f) {
            return;
        }

        float progress =
                Mth.clamp(
                        breakupProgress,
                        0.0f,
                        1.0f
                );

        float horizontalGapProgress =
                Mth.clamp(
                        progress
                                / 0.34f,
                        0.0f,
                        1.0f
                );

        horizontalGapProgress =
                smoothStep(
                        horizontalGapProgress
                );

        float verticalSplitProgress =
                Mth.clamp(
                        progress
                                / 0.42f,
                        0.0f,
                        1.0f
                );

        verticalSplitProgress =
                smoothStep(
                        verticalSplitProgress
                );

        float halfWidthScale =
                Mth.lerp(
                        verticalSplitProgress,
                        0.5f,
                        0.43f
                );

        float halfOffset =
                Mth.lerp(
                        verticalSplitProgress,
                        0.25f,
                        0.285f
                );

        float fallProgress =
                smoothStep(
                        progress
                );

        if (dripStyle == FoundryFaucetDripStyle.HORIZONTAL_ONLY) {
            renderFullWidthDripPiece(
                    consumer,
                    pose,
                    fieldBottomY,
                    fieldHeight,
                    Mth.lerp(
                            horizontalGapProgress,
                            0.0f,
                            0.00f
                    ),
                    Mth.lerp(
                            horizontalGapProgress,
                            0.3333f,
                            0.18f
                    ),
                    easedFall(
                            fallProgress,
                            0.82f
                    ),
                    disappearY,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            renderFullWidthDripPiece(
                    consumer,
                    pose,
                    fieldBottomY,
                    fieldHeight,
                    Mth.lerp(
                            horizontalGapProgress,
                            0.3333f,
                            0.40f
                    ),
                    Mth.lerp(
                            horizontalGapProgress,
                            0.6667f,
                            0.59f
                    ),
                    easedFall(
                            fallProgress,
                            1.0f
                    ),
                    disappearY,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            renderFullWidthDripPiece(
                    consumer,
                    pose,
                    fieldBottomY,
                    fieldHeight,
                    Mth.lerp(
                            horizontalGapProgress,
                            0.6667f,
                            0.79f
                    ),
                    Mth.lerp(
                            horizontalGapProgress,
                            1.0f,
                            1.0f
                    ),
                    easedFall(
                            fallProgress,
                            1.20f
                    ),
                    disappearY,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            return;
        }

        renderDripRow(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                Mth.lerp(
                        horizontalGapProgress,
                        0.0f,
                        0.00f
                ),
                Mth.lerp(
                        horizontalGapProgress,
                        0.3333f,
                        0.18f
                ),
                easedFall(
                        fallProgress,
                        0.82f
                ),
                disappearY,
                halfWidthScale,
                halfOffset,
                fallProgress,
                -0.018f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderDripRow(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                Mth.lerp(
                        horizontalGapProgress,
                        0.3333f,
                        0.40f
                ),
                Mth.lerp(
                        horizontalGapProgress,
                        0.6667f,
                        0.59f
                ),
                easedFall(
                        fallProgress,
                        1.0f
                ),
                disappearY,
                halfWidthScale,
                halfOffset,
                fallProgress,
                0.012f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderDripRow(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                Mth.lerp(
                        horizontalGapProgress,
                        0.6667f,
                        0.79f
                ),
                Mth.lerp(
                        horizontalGapProgress,
                        1.0f,
                        1.0f
                ),
                easedFall(
                        fallProgress,
                        1.20f
                ),
                disappearY,
                halfWidthScale,
                halfOffset,
                fallProgress,
                -0.008f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderFullWidthDripPiece(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fieldBottomY,
            float fieldHeight,
            float normalizedBottom,
            float normalizedTop,
            float fallProgress,
            float disappearY,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        renderFallingDripPiece(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                normalizedBottom,
                normalizedTop,
                fallProgress,
                disappearY,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderDripRow(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fieldBottomY,
            float fieldHeight,
            float normalizedBottom,
            float normalizedTop,
            float rowFallProgress,
            float disappearY,
            float halfWidthScale,
            float halfOffset,
            float fallProgress,
            float rowVariation,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        renderFallingDripPiece(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                normalizedBottom,
                normalizedTop,
                easedFall(
                        rowFallProgress,
                        1.035f
                                + rowVariation
                ),
                disappearY,
                -halfOffset,
                halfWidthScale,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderFallingDripPiece(
                consumer,
                pose,
                fieldBottomY,
                fieldHeight,
                normalizedBottom,
                normalizedTop,
                easedFall(
                        rowFallProgress,
                        0.965f
                                + rowVariation * 0.25f
                ),
                disappearY,
                halfOffset,
                halfWidthScale,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderFallingDripPiece(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fieldBottomY,
            float fieldHeight,
            float normalizedBottom,
            float normalizedTop,
            float fallProgress,
            float disappearY,
            float normalizedXOffset,
            float widthScale,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float initialPieceBottomY =
                fieldBottomY
                        + fieldHeight
                        * normalizedBottom;

        float initialPieceTopY =
                fieldBottomY
                        + fieldHeight
                        * normalizedTop;

        float pieceHeight =
                initialPieceTopY
                        - initialPieceBottomY;

        if (pieceHeight <= 0.01f / 16.0f) {
            return;
        }

        float clampedFallProgress =
                Mth.clamp(
                        fallProgress,
                        0.0f,
                        1.0f
                );

        float requiredFallDistance =
                Math.max(
                        0.0f,
                        initialPieceTopY
                                - disappearY
                                + 0.02f / 16.0f
                );

        float actualFallDistance =
                requiredFallDistance
                        * clampedFallProgress;

        float pieceBottomY =
                initialPieceBottomY
                        - actualFallDistance;

        float pieceTopY =
                initialPieceTopY
                        - actualFallDistance;

        pieceBottomY =
                Math.max(
                        pieceBottomY,
                        disappearY
                );

        if (
                pieceTopY
                        - pieceBottomY
                        <= 0.01f / 16.0f
        ) {
            return;
        }

        renderStream(
                consumer,
                pose,
                pieceBottomY,
                pieceTopY,
                pieceTopY,
                widthScale,
                1.0f,
                normalizedXOffset,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static float easedFall(
            float progress,
            float speed
    ) {
        float clampedProgress =
                Mth.clamp(
                        progress,
                        0.0f,
                        1.0f
                );

        float safeSpeed =
                Math.max(
                        0.05f,
                        speed
                );

        return 1.0f
                - (float) Math.pow(
                1.0f
                        - clampedProgress,
                safeSpeed
        );
    }

    private static float smoothStep(
            float value
    ) {
        float clamped =
                Mth.clamp(
                        value,
                        0.0f,
                        1.0f
                );

        return clamped
                * clamped
                * (
                3.0f
                        - 2.0f
                        * clamped
        );
    }

    private static void renderChannelLiquid(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float channelOuterZ,
            float channelInnerZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float channelDepthUv =
                channelInnerZ
                        - channelOuterZ;

        if (channelDepthUv <= 0.0001f) {
            return;
        }

        addVertex(
                consumer,
                pose,
                CHANNEL_MIN_X,
                CHANNEL_TOP_Y,
                channelOuterZ,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                CHANNEL_MIN_X,
                CHANNEL_TOP_Y,
                channelInnerZ,
                0.0f,
                channelDepthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                CHANNEL_MAX_X,
                CHANNEL_TOP_Y,
                channelInnerZ,
                CHANNEL_WIDTH_UV,
                channelDepthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                CHANNEL_MAX_X,
                CHANNEL_TOP_Y,
                channelOuterZ,
                CHANNEL_WIDTH_UV,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MIN_X,
                channelOuterZ,
                CHANNEL_MAX_X,
                channelOuterZ,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                CHANNEL_WIDTH_UV,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MAX_X,
                channelInnerZ,
                CHANNEL_MIN_X,
                channelInnerZ,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                CHANNEL_WIDTH_UV,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MIN_X,
                channelInnerZ,
                CHANNEL_MIN_X,
                channelOuterZ,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                channelDepthUv,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MAX_X,
                channelOuterZ,
                CHANNEL_MAX_X,
                channelInnerZ,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                channelDepthUv,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderStream(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float bottomY,
            float innerTopY,
            float outerTopY,
            float widthScale,
            float depthScale,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        renderStream(
                consumer,
                pose,
                bottomY,
                innerTopY,
                outerTopY,
                widthScale,
                depthScale,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderStream(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float bottomY,
            float innerTopY,
            float outerTopY,
            float widthScale,
            float depthScale,
            float normalizedXOffset,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float originalStreamWidth =
                STREAM_MAX_X
                        - STREAM_MIN_X;

        float centerX =
                (
                        STREAM_MIN_X
                                + STREAM_MAX_X
                )
                        * 0.5f
                        + normalizedXOffset
                        * originalStreamWidth;

        float centerZ =
                (
                        STREAM_MIN_Z
                                + STREAM_MAX_Z
                )
                        * 0.5f;

        float halfWidth =
                (
                        STREAM_MAX_X
                                - STREAM_MIN_X
                )
                        * 0.5f
                        * Mth.clamp(
                        widthScale,
                        0.0f,
                        1.0f
                );

        float halfDepth =
                (
                        STREAM_MAX_Z
                                - STREAM_MIN_Z
                )
                        * 0.5f
                        * Mth.clamp(
                        depthScale,
                        0.0f,
                        1.0f
                );

        float minX =
                centerX
                        - halfWidth;

        float maxX =
                centerX
                        + halfWidth;

        float minZ =
                centerZ
                        - halfDepth;

        float maxZ =
                centerZ
                        + halfDepth;

        float streamWidthUv =
                maxX
                        - minX;

        float streamDepthUv =
                maxZ
                        - minZ;

        renderRepeatedVerticalFace(
                consumer,
                pose,
                minX,
                minZ,
                maxX,
                minZ,
                bottomY,
                outerTopY,
                streamWidthUv,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                maxX,
                maxZ,
                minX,
                maxZ,
                bottomY,
                outerTopY,
                streamWidthUv,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                minX,
                maxZ,
                minX,
                minZ,
                bottomY,
                outerTopY,
                streamDepthUv,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderRepeatedVerticalFace(
                consumer,
                pose,
                maxX,
                minZ,
                maxX,
                maxZ,
                bottomY,
                outerTopY,
                streamDepthUv,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        renderSlopedStreamTop(
                consumer,
                pose,
                minX,
                maxX,
                minZ,
                maxZ,
                innerTopY,
                outerTopY,
                streamWidthUv,
                streamDepthUv,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderSlopedStreamTop(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float innerTopY,
            float outerTopY,
            float streamWidthUv,
            float streamDepthUv,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float wedgeHeight =
                innerTopY
                        - outerTopY;

        if (wedgeHeight > 0.00001f) {
            renderRepeatedVerticalFace(
                    consumer,
                    pose,
                    maxX,
                    maxZ,
                    minX,
                    maxZ,
                    outerTopY,
                    innerTopY,
                    streamWidthUv,
                    0.0f,
                    0.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    minX,
                    outerTopY,
                    maxZ,
                    0.0f,
                    wedgeHeight,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    minX,
                    outerTopY,
                    minZ,
                    streamDepthUv,
                    wedgeHeight,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    minX,
                    outerTopY,
                    minZ,
                    streamDepthUv,
                    wedgeHeight,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    minX,
                    innerTopY,
                    maxZ,
                    0.0f,
                    0.0f,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    maxX,
                    outerTopY,
                    minZ,
                    0.0f,
                    wedgeHeight,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    maxX,
                    outerTopY,
                    maxZ,
                    streamDepthUv,
                    wedgeHeight,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    maxX,
                    innerTopY,
                    maxZ,
                    streamDepthUv,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    maxX,
                    outerTopY,
                    minZ,
                    0.0f,
                    wedgeHeight,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }

        addVertex(
                consumer,
                pose,
                minX,
                outerTopY,
                minZ,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                innerTopY,
                maxZ,
                0.0f,
                streamDepthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                innerTopY,
                maxZ,
                streamWidthUv,
                streamDepthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                outerTopY,
                minZ,
                streamWidthUv,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void renderRepeatedVerticalFace(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float firstX,
            float firstZ,
            float secondX,
            float secondZ,
            float bottomY,
            float topY,
            float uvWidth,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        if (topY <= bottomY) {
            return;
        }

        float segmentTop =
                topY;

        while (segmentTop > bottomY + 0.00001f) {
            float segmentBottom =
                    Math.max(
                            bottomY,
                            segmentTop - 1.0f
                    );

            float segmentHeight =
                    segmentTop - segmentBottom;

            addVertex(
                    consumer,
                    pose,
                    firstX,
                    segmentBottom,
                    firstZ,
                    0.0f,
                    segmentHeight,
                    normalX,
                    normalY,
                    normalZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    secondX,
                    segmentBottom,
                    secondZ,
                    uvWidth,
                    segmentHeight,
                    normalX,
                    normalY,
                    normalZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    secondX,
                    segmentTop,
                    secondZ,
                    uvWidth,
                    0.0f,
                    normalX,
                    normalY,
                    normalZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            addVertex(
                    consumer,
                    pose,
                    firstX,
                    segmentTop,
                    firstZ,
                    0.0f,
                    0.0f,
                    normalX,
                    normalY,
                    normalZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            segmentTop =
                    segmentBottom;
        }
    }

    private static void addVertex(
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
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        alpha
                )
                .setUv(
                        u,
                        Mth.lerp(
                                v,
                                frameMinV,
                                frameMaxV
                        )
                )
                .setOverlay(
                        packedOverlay
                )
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
