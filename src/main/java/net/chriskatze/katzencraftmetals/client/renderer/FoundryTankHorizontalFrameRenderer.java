package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.core.Direction;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CORNER_CAP_U0;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CORNER_CAP_U1;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CORNER_CAP_V0;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CORNER_CAP_V1;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.PIXEL;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_PERIMETER_WIDTH;

/**
 * Renders top/bottom connected frame pieces and corner caps.
 */
final class FoundryTankHorizontalFrameRenderer {

    private FoundryTankHorizontalFrameRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        boolean isolatedHorizontalTank =
                !FoundryTankVisualConnections.hasAnyHorizontalNeighbor(
                        tank
                );

        if (isolatedHorizontalTank) {
            FoundryTankCasingQuads.renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f,
                    packedLight,
                    packedOverlay
            );

            return;
        }

        boolean northBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.NORTH
                );

        boolean southBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.SOUTH
                );

        boolean westBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.WEST
                );

        boolean eastBoundary =
                !FoundryTankVisualConnections.hasAdjacentExposedFace(
                        tank,
                        face,
                        Direction.EAST
                );

        if (northBoundary) {
            FoundryTankCasingQuads.renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    0.0f,
                    TOP_PERIMETER_WIDTH,
                    0.0f,
                    1.0f,
                    0.0f,
                    TOP_PERIMETER_WIDTH,
                    packedLight,
                    packedOverlay
            );
        }

        if (southBoundary) {
            FoundryTankCasingQuads.renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    1.0f - TOP_PERIMETER_WIDTH,
                    1.0f,
                    0.0f,
                    1.0f,
                    1.0f - TOP_PERIMETER_WIDTH,
                    1.0f,
                    packedLight,
                    packedOverlay
            );
        }

        float sideMinZ =
                northBoundary
                        ? TOP_PERIMETER_WIDTH
                        : 0.0f;

        float sideMaxZ =
                southBoundary
                        ? 1.0f - TOP_PERIMETER_WIDTH
                        : 1.0f;

        if (westBoundary && sideMaxZ > sideMinZ) {
            FoundryTankCasingQuads.renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    TOP_PERIMETER_WIDTH,
                    sideMinZ,
                    sideMaxZ,
                    0.0f,
                    TOP_PERIMETER_WIDTH,
                    sideMinZ,
                    sideMaxZ,
                    packedLight,
                    packedOverlay
            );
        }

        if (eastBoundary && sideMaxZ > sideMinZ) {
            FoundryTankCasingQuads.renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    1.0f - TOP_PERIMETER_WIDTH,
                    1.0f,
                    sideMinZ,
                    sideMaxZ,
                    1.0f - TOP_PERIMETER_WIDTH,
                    1.0f,
                    sideMinZ,
                    sideMaxZ,
                    packedLight,
                    packedOverlay
            );
        }

        /*
         * Close concave perimeter turns.
         *
         * Example footprint:
         *
         *   [Tank][Tank]
         *   [Tank][ air]
         *
         * The north and west perimeter runs meet at the missing diagonal
         * block. Without this explicit 1x1 cap, that junction leaves the
         * exact hole visible in the screenshot.
         */
        renderHorizontalCornerCap(
                tank,
                face,
                Direction.NORTH,
                Direction.WEST,
                0.0f,
                PIXEL,
                0.0f,
                PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tank,
                face,
                Direction.NORTH,
                Direction.EAST,
                1.0f - PIXEL,
                1.0f,
                0.0f,
                PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tank,
                face,
                Direction.SOUTH,
                Direction.WEST,
                0.0f,
                PIXEL,
                1.0f - PIXEL,
                1.0f,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tank,
                face,
                Direction.SOUTH,
                Direction.EAST,
                1.0f - PIXEL,
                1.0f,
                1.0f - PIXEL,
                1.0f,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    /**
     * Fills the four possible one-pixel gaps around the base of a Tank that
     * rises above a wider lower layer.
     *
     * Important geometry detail:
     *
     * The missing square is not inside the raised Tank's footprint. It is in
     * the diagonal lower Tank immediately outside each raised corner.
     */
    static void renderRaisedFootprintCornerCaps(
            FoundryTankBlockEntity tank,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        FoundryTankBlockEntity tankBelow =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        Direction.DOWN
                );

        if (tankBelow == null) {
            return;
        }

        renderRaisedFootprintCornerCap(
                tank,
                tankBelow,
                Direction.NORTH,
                Direction.WEST,
                -PIXEL,
                0.0f,
                -PIXEL,
                0.0f,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                tank,
                tankBelow,
                Direction.NORTH,
                Direction.EAST,
                1.0f,
                1.0f + PIXEL,
                -PIXEL,
                0.0f,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                tank,
                tankBelow,
                Direction.SOUTH,
                Direction.WEST,
                -PIXEL,
                0.0f,
                1.0f,
                1.0f + PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                tank,
                tankBelow,
                Direction.SOUTH,
                Direction.EAST,
                1.0f,
                1.0f + PIXEL,
                1.0f,
                1.0f + PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    /**
     * Adds the missing one-pixel square at a concave top/bottom perimeter
     * corner.
     *
     * The cap is needed only when both orthogonal neighboring Tanks belong to
     * the same component and the diagonal Tank does not.
     */
    private static void renderHorizontalCornerCap(
            FoundryTankBlockEntity tank,
            Direction face,
            Direction firstDirection,
            Direction secondDirection,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        FoundryTankBlockEntity firstNeighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        firstDirection
                );

        FoundryTankBlockEntity secondNeighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        secondDirection
                );

        if (
                firstNeighbor == null
                        || secondNeighbor == null
        ) {
            return;
        }

        FoundryTankBlockEntity diagonalNeighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        firstNeighbor,
                        secondDirection
                );

        if (diagonalNeighbor != null) {
            return;
        }

        FoundryTankCasingQuads.renderHorizontalRect(
                face,
                consumer,
                pose,
                minX,
                maxX,
                minZ,
                maxZ,
                CORNER_CAP_U0,
                CORNER_CAP_U1,
                CORNER_CAP_V0,
                CORNER_CAP_V1,
                packedLight,
                packedOverlay
        );
    }

    private static void renderRaisedFootprintCornerCap(
            FoundryTankBlockEntity raisedTank,
            FoundryTankBlockEntity tankBelow,
            Direction firstDirection,
            Direction secondDirection,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        /*
         * The raised Tank corner must actually be exposed at this level.
         */
        if (
                FoundryTankVisualConnections.isSameComponent(
                        raisedTank,
                        firstDirection
                )
                        || FoundryTankVisualConnections.isSameComponent(
                        raisedTank,
                        secondDirection
                )
        ) {
            return;
        }

        FoundryTankBlockEntity firstLowerNeighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tankBelow,
                        firstDirection
                );

        FoundryTankBlockEntity secondLowerNeighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tankBelow,
                        secondDirection
                );

        if (
                firstLowerNeighbor == null
                        || secondLowerNeighbor == null
        ) {
            return;
        }

        /*
         * Require the diagonal lower Tank as well. The cap is physically
         * located on top of this diagonal Tank.
         */
        FoundryTankBlockEntity diagonalLowerTank =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        firstLowerNeighbor,
                        secondDirection
                );

        if (diagonalLowerTank == null) {
            return;
        }

        renderRaisedBaseCornerCap(
                consumer,
                pose,
                minX,
                maxX,
                minZ,
                maxZ,
                packedLight,
                packedOverlay
        );
    }

    /**
     * Renders one upward-facing 1x1 square on the exact shared plane between
     * the lower layer and the raised Tank.
     *
     * X and Z are allowed to extend one pixel outside the raised Tank's local
     * block bounds because the cap belongs to the neighboring diagonal lower
     * Tank.
     */
    private static void renderRaisedBaseCornerCap(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            int packedLight,
            int packedOverlay
    ) {
        float y =
                0.0f;

        FoundryTankCasingQuads.renderCasingQuad(
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
                CORNER_CAP_U0,
                CORNER_CAP_V0,
                CORNER_CAP_U0,
                CORNER_CAP_V1,
                CORNER_CAP_U1,
                CORNER_CAP_V1,
                CORNER_CAP_U1,
                CORNER_CAP_V0,
                0.0f,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay
        );
    }
}
