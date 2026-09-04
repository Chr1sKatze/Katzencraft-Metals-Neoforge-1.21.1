package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

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

    /** Legacy bridge so the unregistered old Tank BER source still compiles. */
    static void render(
            FoundryTankBlockEntity tank,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (tank == null) {
            return;
        }

        Set<BlockPos> structure = resolveLegacyStructure(tank);
        render(
                tank.getBlockPos(),
                structure,
                face,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    static void render(
            BlockPos tankPos,
            Set<BlockPos> structure,
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        boolean isolatedHorizontalTank =
                !hasAnyHorizontalNeighbor(
                        structure,
                        tankPos
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
                !hasAdjacentExposedFace(
                        structure,
                        tankPos,
                        face,
                        Direction.NORTH
                );

        boolean southBoundary =
                !hasAdjacentExposedFace(
                        structure,
                        tankPos,
                        face,
                        Direction.SOUTH
                );

        boolean westBoundary =
                !hasAdjacentExposedFace(
                        structure,
                        tankPos,
                        face,
                        Direction.WEST
                );

        boolean eastBoundary =
                !hasAdjacentExposedFace(
                        structure,
                        tankPos,
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
                tankPos,
                structure,
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
                tankPos,
                structure,
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
                tankPos,
                structure,
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
                tankPos,
                structure,
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
    /** Legacy bridge so FoundryTankCasingRenderer remains source-compatible. */
    static void renderRaisedFootprintCornerCaps(
            FoundryTankBlockEntity tank,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (tank == null) {
            return;
        }

        renderRaisedFootprintCornerCaps(
                tank.getBlockPos(),
                resolveLegacyStructure(tank),
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static Set<BlockPos> resolveLegacyStructure(
            FoundryTankBlockEntity tank
    ) {
        Set<BlockPos> structure = Set.of(tank.getBlockPos().immutable());

        if (tank.getLevel() != null) {
            FoundryTankNetwork network =
                    FoundryTankNetwork.find(
                            tank.getLevel(),
                            tank.getBlockPos()
                    );

            if (network != null) {
                structure = network.getTankPositions();
            }
        }

        return structure;
    }

    static void renderRaisedFootprintCornerCaps(
            BlockPos tankPos,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        BlockPos tankBelow =
                sameComponentNeighbor(
                        structure,
                        tankPos,
                        Direction.DOWN
                );

        if (tankBelow == null) {
            return;
        }

        renderRaisedFootprintCornerCap(
                tankPos,
                tankBelow,
                structure,
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
                tankPos,
                tankBelow,
                structure,
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
                tankPos,
                tankBelow,
                structure,
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
                tankPos,
                tankBelow,
                structure,
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
            BlockPos tankPos,
            Set<BlockPos> structure,
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
        BlockPos firstNeighbor =
                sameComponentNeighbor(
                        structure,
                        tankPos,
                        firstDirection
                );

        BlockPos secondNeighbor =
                sameComponentNeighbor(
                        structure,
                        tankPos,
                        secondDirection
                );

        if (
                firstNeighbor == null
                        || secondNeighbor == null
        ) {
            return;
        }

        BlockPos diagonalNeighbor =
                sameComponentNeighbor(
                        structure,
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
            BlockPos raisedTank,
            BlockPos tankBelow,
            Set<BlockPos> structure,
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
                sameComponentNeighbor(
                        structure,
                        raisedTank,
                        firstDirection
                ) != null
                        || sameComponentNeighbor(
                        structure,
                        raisedTank,
                        secondDirection
                ) != null
        ) {
            return;
        }

        BlockPos firstLowerNeighbor =
                sameComponentNeighbor(
                        structure,
                        tankBelow,
                        firstDirection
                );

        BlockPos secondLowerNeighbor =
                sameComponentNeighbor(
                        structure,
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
        BlockPos diagonalLowerTank =
                sameComponentNeighbor(
                        structure,
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

    private static boolean hasAnyHorizontalNeighbor(
            Set<BlockPos> structure,
            BlockPos tankPos
    ) {
        return structure.contains(tankPos.north())
                || structure.contains(tankPos.south())
                || structure.contains(tankPos.east())
                || structure.contains(tankPos.west());
    }

    private static boolean hasAdjacentExposedFace(
            Set<BlockPos> structure,
            BlockPos tankPos,
            Direction faceDirection,
            Direction edgeDirection
    ) {
        BlockPos adjacent = tankPos.relative(edgeDirection);

        return structure.contains(adjacent)
                && !structure.contains(
                adjacent.relative(faceDirection)
        );
    }

    private static BlockPos sameComponentNeighbor(
            Set<BlockPos> structure,
            BlockPos origin,
            Direction direction
    ) {
        if (
                structure == null
                        || origin == null
                        || direction == null
        ) {
            return null;
        }

        BlockPos neighbor = origin.relative(direction);
        return structure.contains(neighbor)
                ? neighbor.immutable()
                : null;
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
