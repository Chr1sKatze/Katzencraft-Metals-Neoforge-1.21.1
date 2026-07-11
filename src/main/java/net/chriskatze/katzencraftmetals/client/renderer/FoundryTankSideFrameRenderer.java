package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.core.Direction;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.MARKER_ROWS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.PIXEL;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SIDE_HORIZONTAL_FRAME_HEIGHT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SIDE_VERTICAL_FRAME_WIDTH;

/**
 * Renders connected vertical side-frame pieces and side marker pixels.
 */
final class FoundryTankSideFrameRenderer {

    private FoundryTankSideFrameRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        Direction leftDirection =
                getFaceLeftDirection(face);

        Direction rightDirection =
                leftDirection.getOpposite();

        boolean topBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.UP
                );

        boolean bottomBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.DOWN
                );

        boolean leftBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        leftDirection
                );

        boolean rightBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        rightDirection
                );

        if (topBoundary) {
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    1.0f - SIDE_HORIZONTAL_FRAME_HEIGHT,
                    1.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    SIDE_HORIZONTAL_FRAME_HEIGHT,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }

        if (bottomBoundary) {
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    0.0f,
                    SIDE_HORIZONTAL_FRAME_HEIGHT,
                    0.0f,
                    1.0f,
                    1.0f - SIDE_HORIZONTAL_FRAME_HEIGHT,
                    1.0f,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }

        float verticalMinY =
                bottomBoundary
                        ? SIDE_HORIZONTAL_FRAME_HEIGHT
                        : 0.0f;

        float verticalMaxY =
                topBoundary
                        ? 1.0f - SIDE_HORIZONTAL_FRAME_HEIGHT
                        : 1.0f;

        float verticalMinV =
                1.0f - verticalMaxY;

        float verticalMaxV =
                1.0f - verticalMinY;

        if (leftBoundary && verticalMaxY > verticalMinY) {
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    SIDE_VERTICAL_FRAME_WIDTH,
                    verticalMinY,
                    verticalMaxY,
                    0.0f,
                    SIDE_VERTICAL_FRAME_WIDTH,
                    verticalMinV,
                    verticalMaxV,
                    packedLight,
                    packedOverlay,
                    0.0f
            );

            renderLeftMarkers(
                    tank,
                    face,
                    consumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        if (rightBoundary && verticalMaxY > verticalMinY) {
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    1.0f - SIDE_VERTICAL_FRAME_WIDTH,
                    1.0f,
                    verticalMinY,
                    verticalMaxY,
                    1.0f - SIDE_VERTICAL_FRAME_WIDTH,
                    1.0f,
                    verticalMinV,
                    verticalMaxV,
                    packedLight,
                    packedOverlay,
                    0.0f
            );

            renderRightMarkers(
                    tank,
                    face,
                    consumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static void renderLeftMarkers(
            FoundryTankBlockEntity tank,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        for (int row : MARKER_ROWS) {
            float topMinY =
                    1.0f - (row + 1.0f) * PIXEL;

            float topMaxY =
                    1.0f - row * PIXEL;

            /*
             * Two-pixel arm.
             */
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    PIXEL,
                    3.0f * PIXEL,
                    topMinY,
                    topMaxY,
                    PIXEL,
                    3.0f * PIXEL,
                    row * PIXEL,
                    (row + 1.0f) * PIXEL,
                    packedLight,
                    packedOverlay,
                    0.0f
            );

            float lowerMinY =
                    1.0f - (row + 2.0f) * PIXEL;

            float lowerMaxY =
                    1.0f - (row + 1.0f) * PIXEL;

            /*
             * One-pixel piece directly underneath.
             */
            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    PIXEL,
                    2.0f * PIXEL,
                    lowerMinY,
                    lowerMaxY,
                    PIXEL,
                    2.0f * PIXEL,
                    (row + 1.0f) * PIXEL,
                    (row + 2.0f) * PIXEL,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }

        renderMarkerJoin(
                tank,
                face,
                true,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderRightMarkers(
            FoundryTankBlockEntity tank,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        for (int row : MARKER_ROWS) {
            float topMinY =
                    1.0f - (row + 1.0f) * PIXEL;

            float topMaxY =
                    1.0f - row * PIXEL;

            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    13.0f * PIXEL,
                    15.0f * PIXEL,
                    topMinY,
                    topMaxY,
                    13.0f * PIXEL,
                    15.0f * PIXEL,
                    row * PIXEL,
                    (row + 1.0f) * PIXEL,
                    packedLight,
                    packedOverlay,
                    0.0f
            );

            float lowerMinY =
                    1.0f - (row + 2.0f) * PIXEL;

            float lowerMaxY =
                    1.0f - (row + 1.0f) * PIXEL;

            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    14.0f * PIXEL,
                    15.0f * PIXEL,
                    lowerMinY,
                    lowerMaxY,
                    14.0f * PIXEL,
                    15.0f * PIXEL,
                    (row + 1.0f) * PIXEL,
                    (row + 2.0f) * PIXEL,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }

        renderMarkerJoin(
                tank,
                face,
                false,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    /**
     * Reproduces the edge pixels from the supplied stacked reference texture.
     *
     * A lower Tank contributes one 1x1 extension at its top edge.
     * The Tank above contributes the two-pixel extension at its bottom edge.
     */
    private static void renderMarkerJoin(
            FoundryTankBlockEntity tank,
            Direction face,
            boolean leftSide,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        boolean continuesAbove =
                FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.UP
                );

        boolean continuesBelow =
                FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.DOWN
                );

        if (continuesAbove) {
            float minHorizontal =
                    leftSide
                            ? PIXEL
                            : 14.0f * PIXEL;

            float maxHorizontal =
                    leftSide
                            ? 2.0f * PIXEL
                            : 15.0f * PIXEL;

            float minU =
                    leftSide
                            ? PIXEL
                            : 14.0f * PIXEL;

            float maxU =
                    leftSide
                            ? 2.0f * PIXEL
                            : 15.0f * PIXEL;

            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    minHorizontal,
                    maxHorizontal,
                    15.0f * PIXEL,
                    1.0f,
                    minU,
                    maxU,
                    0.0f,
                    PIXEL,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }

        if (continuesBelow) {
            float minHorizontal =
                    leftSide
                            ? PIXEL
                            : 13.0f * PIXEL;

            float maxHorizontal =
                    leftSide
                            ? 3.0f * PIXEL
                            : 15.0f * PIXEL;

            float minU =
                    leftSide
                            ? PIXEL
                            : 13.0f * PIXEL;

            float maxU =
                    leftSide
                            ? 3.0f * PIXEL
                            : 15.0f * PIXEL;

            FoundryTankCasingQuads.renderSideRect(
                    face,
                    consumer,
                    pose,
                    minHorizontal,
                    maxHorizontal,
                    0.0f,
                    PIXEL,
                    minU,
                    maxU,
                    15.0f * PIXEL,
                    1.0f,
                    packedLight,
                    packedOverlay,
                    0.0f
            );
        }
    }

    private static Direction getFaceLeftDirection(
            Direction face
    ) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException(
                    "Foundry Tank side face must be horizontal."
            );
        };
    }
}
