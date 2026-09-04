package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Draws only the Tank frame pieces that cannot be represented by the six
 * face-adjacency BlockState properties.
 *
 * Ordinary Tank casing remains chunk-baked. This renderer is intentionally a
 * correction layer for concave/stepped boundaries that require diagonal
 * structure knowledge. A rectangular vessel therefore emits no correction
 * quads at all.
 */
final class FoundryControllerTankFrameCorrectionRenderer {

    private static final ResourceLocation SIDE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_side.png"
            );

    private static final ResourceLocation TOP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_top.png"
            );

    private static final float PIXEL = 1.0f / 16.0f;
    private static final float SIDE_CORRECTION_OFFSET = 0.0010f;
    private static final float HORIZONTAL_CORRECTION_OFFSET = 0.0010f;
    private static final float SIDE_MARKER_WIDTH = 3.0f * PIXEL;
    private static final float SIDE_MARKER_RIGHT_MIN = 13.0f * PIXEL;

    private static final float CORNER_CAP_U0 = 7.0f * PIXEL;
    private static final float CORNER_CAP_U1 = 8.0f * PIXEL;
    private static final float CORNER_CAP_V0 = 0.0f;
    private static final float CORNER_CAP_V1 = PIXEL;

    void render(
            FoundryControllerBlockEntity controller,
            Set<BlockPos> structure,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (structure == null || structure.isEmpty()) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();

        /*
         * IMPORTANT:
         *
         * MultiBufferSource.BufferSource may finish the previously requested
         * non-fixed RenderType when getBuffer() is called for another type.
         *
         * v2 requested SIDE_TEXTURE and TOP_TEXTURE up front, then wrote to the
         * side consumer afterwards. In a concave Controller-owned vessel that
         * produced BufferBuilder "Not building!" as soon as the first side
         * correction vertex was submitted.
         *
         * Keep these as two strict phases. Finish submitting every side
         * correction before requesting the top/bottom RenderType.
         *
         * Side corrections deliberately use a no-cull RenderType. The proven
         * historical casing quad helper was authored for two-sided rendering,
         * and a concave frame must be visible from either side of the glass.
         * The v3 culled pass could therefore hide otherwise-correct inner
         * borders depending on the viewing side.
         */
        VertexConsumer sideConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(SIDE_TEXTURE)
                );

        for (BlockPos tankPos : structure) {
            poseStack.pushPose();

            try {
                poseStack.translate(
                        tankPos.getX() - controllerPos.getX(),
                        tankPos.getY() - controllerPos.getY(),
                        tankPos.getZ() - controllerPos.getZ()
                );

                renderSideCorrections(
                        tankPos,
                        structure,
                        sideConsumer,
                        poseStack.last(),
                        packedLight,
                        packedOverlay
                );
            } finally {
                poseStack.popPose();
            }
        }

        /*
         * Request the second RenderType only after all side vertices have been
         * submitted.
         */
        VertexConsumer topConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(TOP_TEXTURE)
                );

        for (BlockPos tankPos : structure) {
            poseStack.pushPose();

            try {
                poseStack.translate(
                        tankPos.getX() - controllerPos.getX(),
                        tankPos.getY() - controllerPos.getY(),
                        tankPos.getZ() - controllerPos.getZ()
                );

                renderHorizontalCorrections(
                        tankPos,
                        structure,
                        topConsumer,
                        poseStack.last(),
                        packedLight,
                        packedOverlay
                );
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderSideCorrections(
            BlockPos tankPos,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (structure.contains(tankPos.relative(face))) {
                continue;
            }

            Direction leftDirection = getFaceLeftDirection(face);
            Direction rightDirection = leftDirection.getOpposite();

            boolean topBoundary =
                    !hasAdjacentExposedFace(
                            tankPos,
                            face,
                            Direction.UP,
                            structure
                    );

            boolean bottomBoundary =
                    !hasAdjacentExposedFace(
                            tankPos,
                            face,
                            Direction.DOWN,
                            structure
                    );

            boolean leftBoundary =
                    !hasAdjacentExposedFace(
                            tankPos,
                            face,
                            leftDirection,
                            structure
                    );

            boolean rightBoundary =
                    !hasAdjacentExposedFace(
                            tankPos,
                            face,
                            rightDirection,
                            structure
                    );

            /*
             * The static multipart model already draws a boundary when the
             * adjacent Tank is absent. We draw only the second case from the
             * historical rule: the adjacent Tank exists, but its matching face
             * is occluded by a diagonal Tank.
             */
            if (needsExposedFaceBoundaryCorrection(
                    tankPos,
                    face,
                    Direction.UP,
                    structure
            )) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        0.0f,
                        1.0f,
                        1.0f - PIXEL,
                        1.0f,
                        0.0f,
                        1.0f,
                        0.0f,
                        PIXEL,
                        packedLight,
                        packedOverlay
                );
            }

            if (needsExposedFaceBoundaryCorrection(
                    tankPos,
                    face,
                    Direction.DOWN,
                    structure
            )) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        0.0f,
                        1.0f,
                        0.0f,
                        PIXEL,
                        0.0f,
                        1.0f,
                        1.0f - PIXEL,
                        1.0f,
                        packedLight,
                        packedOverlay
                );
            }

            float verticalMinY = bottomBoundary ? PIXEL : 0.0f;
            float verticalMaxY = topBoundary ? 1.0f - PIXEL : 1.0f;
            float verticalMinV = 1.0f - verticalMaxY;
            float verticalMaxV = 1.0f - verticalMinY;

            if (
                    leftBoundary
                            && verticalMaxY > verticalMinY
                            && needsExposedFaceBoundaryCorrection(
                            tankPos,
                            face,
                            leftDirection,
                            structure
                    )
            ) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        0.0f,
                        SIDE_MARKER_WIDTH,
                        verticalMinY,
                        verticalMaxY,
                        0.0f,
                        SIDE_MARKER_WIDTH,
                        verticalMinV,
                        verticalMaxV,
                        packedLight,
                        packedOverlay
                );
            }

            if (
                    rightBoundary
                            && verticalMaxY > verticalMinY
                            && needsExposedFaceBoundaryCorrection(
                            tankPos,
                            face,
                            rightDirection,
                            structure
                    )
            ) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        SIDE_MARKER_RIGHT_MIN,
                        1.0f,
                        verticalMinY,
                        verticalMaxY,
                        SIDE_MARKER_RIGHT_MIN,
                        1.0f,
                        verticalMinV,
                        verticalMaxV,
                        packedLight,
                        packedOverlay
                );
            }
        }
    }

    private static void renderHorizontalCorrections(
            BlockPos tankPos,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (!structure.contains(tankPos.above())) {
            renderHorizontalFaceCorrections(
                    tankPos,
                    Direction.UP,
                    structure,
                    consumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        if (!structure.contains(tankPos.below())) {
            renderHorizontalFaceCorrections(
                    tankPos,
                    Direction.DOWN,
                    structure,
                    consumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        } else {
            renderRaisedFootprintCornerCaps(
                    tankPos,
                    structure,
                    consumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static void renderHorizontalFaceCorrections(
            BlockPos tankPos,
            Direction face,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        boolean northBoundary =
                !hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.NORTH,
                        structure
                );

        boolean southBoundary =
                !hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.SOUTH,
                        structure
                );

        /*
         * NORTH/SOUTH own the horizontal corner pixels. WEST/EAST correction
         * strips are trimmed around any north/south perimeter, exactly like
         * the historical dynamic renderer. This keeps dynamic stepped-frame
         * corrections from becoming another source of coplanar corner quads.
         */
        renderHorizontalBoundaryCorrection(
                tankPos,
                face,
                Direction.NORTH,
                northBoundary,
                southBoundary,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundaryCorrection(
                tankPos,
                face,
                Direction.SOUTH,
                northBoundary,
                southBoundary,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundaryCorrection(
                tankPos,
                face,
                Direction.WEST,
                northBoundary,
                southBoundary,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundaryCorrection(
                tankPos,
                face,
                Direction.EAST,
                northBoundary,
                southBoundary,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        /*
         * True diagonal concave corners still need one explicit opaque pixel.
         * This cap is not a normal convex corner and therefore cannot be
         * represented by the six Tank adjacency BlockState properties.
         */
        renderHorizontalCornerCap(
                tankPos,
                face,
                Direction.NORTH,
                Direction.WEST,
                0.0f,
                PIXEL,
                0.0f,
                PIXEL,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tankPos,
                face,
                Direction.NORTH,
                Direction.EAST,
                1.0f - PIXEL,
                1.0f,
                0.0f,
                PIXEL,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tankPos,
                face,
                Direction.SOUTH,
                Direction.WEST,
                0.0f,
                PIXEL,
                1.0f - PIXEL,
                1.0f,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalCornerCap(
                tankPos,
                face,
                Direction.SOUTH,
                Direction.EAST,
                1.0f - PIXEL,
                1.0f,
                1.0f - PIXEL,
                1.0f,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderHorizontalBoundaryCorrection(
            BlockPos tankPos,
            Direction face,
            Direction edgeDirection,
            boolean northBoundary,
            boolean southBoundary,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (!needsExposedFaceBoundaryCorrection(
                tankPos,
                face,
                edgeDirection,
                structure
        )) {
            return;
        }

        float sideMinZ =
                northBoundary
                        ? PIXEL
                        : 0.0f;

        float sideMaxZ =
                southBoundary
                        ? 1.0f - PIXEL
                        : 1.0f;

        switch (edgeDirection) {
            case NORTH -> renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    0.0f,
                    PIXEL,
                    0.0f,
                    1.0f,
                    0.0f,
                    PIXEL,
                    packedLight,
                    packedOverlay
            );
            case SOUTH -> renderHorizontalRect(
                    face,
                    consumer,
                    pose,
                    0.0f,
                    1.0f,
                    1.0f - PIXEL,
                    1.0f,
                    0.0f,
                    1.0f,
                    1.0f - PIXEL,
                    1.0f,
                    packedLight,
                    packedOverlay
            );
            case WEST -> {
                if (sideMaxZ > sideMinZ) {
                    renderHorizontalRect(
                            face,
                            consumer,
                            pose,
                            0.0f,
                            PIXEL,
                            sideMinZ,
                            sideMaxZ,
                            0.0f,
                            PIXEL,
                            sideMinZ,
                            sideMaxZ,
                            packedLight,
                            packedOverlay
                    );
                }
            }
            case EAST -> {
                if (sideMaxZ > sideMinZ) {
                    renderHorizontalRect(
                            face,
                            consumer,
                            pose,
                            1.0f - PIXEL,
                            1.0f,
                            sideMinZ,
                            sideMaxZ,
                            1.0f - PIXEL,
                            1.0f,
                            sideMinZ,
                            sideMaxZ,
                            packedLight,
                            packedOverlay
                    );
                }
            }
            default -> {
            }
        }
    }

    private static void renderHorizontalCornerCap(
            BlockPos tankPos,
            Direction face,
            Direction firstDirection,
            Direction secondDirection,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (
                !structure.contains(tankPos.relative(firstDirection))
                        || !structure.contains(tankPos.relative(secondDirection))
        ) {
            return;
        }

        BlockPos diagonal =
                tankPos.relative(firstDirection)
                        .relative(secondDirection);

        if (structure.contains(diagonal)) {
            return;
        }

        renderHorizontalRect(
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

    private static void renderRaisedFootprintCornerCaps(
            BlockPos raisedTank,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        BlockPos tankBelow = raisedTank.below();

        renderRaisedFootprintCornerCap(
                raisedTank,
                tankBelow,
                Direction.NORTH,
                Direction.WEST,
                -PIXEL,
                0.0f,
                -PIXEL,
                0.0f,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                raisedTank,
                tankBelow,
                Direction.NORTH,
                Direction.EAST,
                1.0f,
                1.0f + PIXEL,
                -PIXEL,
                0.0f,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                raisedTank,
                tankBelow,
                Direction.SOUTH,
                Direction.WEST,
                -PIXEL,
                0.0f,
                1.0f,
                1.0f + PIXEL,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedFootprintCornerCap(
                raisedTank,
                tankBelow,
                Direction.SOUTH,
                Direction.EAST,
                1.0f,
                1.0f + PIXEL,
                1.0f,
                1.0f + PIXEL,
                structure,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderRaisedFootprintCornerCap(
            BlockPos raisedTank,
            BlockPos tankBelow,
            Direction firstDirection,
            Direction secondDirection,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (
                structure.contains(raisedTank.relative(firstDirection))
                        || structure.contains(raisedTank.relative(secondDirection))
        ) {
            return;
        }

        BlockPos firstLowerNeighbor = tankBelow.relative(firstDirection);
        BlockPos secondLowerNeighbor = tankBelow.relative(secondDirection);

        if (
                !structure.contains(firstLowerNeighbor)
                        || !structure.contains(secondLowerNeighbor)
        ) {
            return;
        }

        BlockPos diagonalLower =
                firstLowerNeighbor.relative(secondDirection);

        if (!structure.contains(diagonalLower)) {
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
        float y = HORIZONTAL_CORRECTION_OFFSET;

        renderQuad(
                consumer,
                pose,
                minX, y, minZ,
                minX, y, maxZ,
                maxX, y, maxZ,
                maxX, y, minZ,
                CORNER_CAP_U0, CORNER_CAP_V0,
                CORNER_CAP_U0, CORNER_CAP_V1,
                CORNER_CAP_U1, CORNER_CAP_V1,
                CORNER_CAP_U1, CORNER_CAP_V0,
                0.0f, 1.0f, 0.0f,
                packedLight,
                packedOverlay
        );
    }

    private static boolean hasAdjacentExposedFace(
            BlockPos tankPos,
            Direction faceDirection,
            Direction edgeDirection,
            Set<BlockPos> structure
    ) {
        BlockPos adjacent = tankPos.relative(edgeDirection);

        return structure.contains(adjacent)
                && !structure.contains(adjacent.relative(faceDirection));
    }

    /**
     * Returns true only for an exposed-face edge that the static six-property
     * multipart model cannot represent.
     *
     * The complete rule for one edge of an exposed face is:
     *   - if the neighboring Tank has the same exposed face, the surface
     *     continues and there is no border;
     *   - if there is no neighboring Tank at all, the static BlockState model
     *     already renders that border;
     *   - if a neighboring Tank exists but its corresponding face is occluded,
     *     exposed-face continuity stops here and this renderer supplies the
     *     missing concave/stepped border.
     *
     * This covers L/U/arch footprints and stepped 3-D layouts without special
     * casing ownership or additional BlockState properties.
     */
    private static boolean needsExposedFaceBoundaryCorrection(
            BlockPos tankPos,
            Direction faceDirection,
            Direction edgeDirection,
            Set<BlockPos> structure
    ) {
        BlockPos adjacent = tankPos.relative(edgeDirection);

        if (!structure.contains(adjacent)) {
            return false;
        }

        return !hasAdjacentExposedFace(
                tankPos,
                faceDirection,
                edgeDirection,
                structure
        );
    }

    private static Direction getFaceLeftDirection(Direction face) {
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

    /**
     * Renders one two-sided correction strip on the exposed Tank face.
     *
     * Only one geometric quad is submitted. The no-cull side RenderType makes
     * that physical frame visible from both sides, matching the historical
     * Tank casing renderer. Seeing the same physical UV mapping from the back
     * naturally reverses the screen-space direction, so the marker arms still
     * point toward the panel rather than requiring a separately mirrored
     * back-face quad.
     */
    private static void renderSideRect(
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
            int packedOverlay
    ) {
        FoundryTankCasingQuads.renderSideRect(
                face,
                consumer,
                pose,
                minHorizontal,
                maxHorizontal,
                minY,
                maxY,
                minU,
                maxU,
                minV,
                maxV,
                packedLight,
                packedOverlay,
                SIDE_CORRECTION_OFFSET
        );
    }

    /**
     * Dynamic horizontal correction quads sit a tiny amount outside the baked
     * block-model plane. This is small enough to be visually imperceptible
     * (0.016 texture pixels) but prevents a stepped correction from depth-
     * fighting a static perimeter end piece that the six BlockState properties
     * cannot know should be suppressed.
     */
    private static void renderHorizontalRect(
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
                        ? 1.0f + HORIZONTAL_CORRECTION_OFFSET
                        : -HORIZONTAL_CORRECTION_OFFSET;

        if (face == Direction.UP) {
            FoundryTankCasingQuads.renderCasingQuad(
                    consumer,
                    pose,
                    minX, y, minZ,
                    minX, y, maxZ,
                    maxX, y, maxZ,
                    maxX, y, minZ,
                    minU, minV,
                    minU, maxV,
                    maxU, maxV,
                    maxU, minV,
                    0.0f, 1.0f, 0.0f,
                    packedLight,
                    packedOverlay
            );
        } else {
            FoundryTankCasingQuads.renderCasingQuad(
                    consumer,
                    pose,
                    minX, y, maxZ,
                    minX, y, minZ,
                    maxX, y, minZ,
                    maxX, y, maxZ,
                    minU, maxV,
                    minU, minV,
                    maxU, minV,
                    maxU, maxV,
                    0.0f, -1.0f, 0.0f,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static void renderQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float u1, float v1,
            float u2, float v2,
            float u3, float v3,
            float u4, float v4,
            float normalX, float normalY, float normalZ,
            int packedLight,
            int packedOverlay
    ) {
        FoundryTankCasingQuads.renderCasingQuad(
                consumer,
                pose,
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3,
                x4, y4, z4,
                u1, v1,
                u2, v2,
                u3, v3,
                u4, v4,
                normalX, normalY, normalZ,
                packedLight,
                packedOverlay
        );
    }
}
