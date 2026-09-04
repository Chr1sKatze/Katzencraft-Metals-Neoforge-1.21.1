package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

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
                    "textures/block/foundry_tank_frame.png"
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

    /*
     * Correction frames should look like the grey metallic rail from the
     * original side texture, not like its deliberately dark shaded opposite
     * edge.
     *
     * foundry_tank_side.png is asymmetric:
     * - U 0..3 contains the bright left rail + marker pattern
     * - U 13..16 contains the same pattern but with very dark shading
     * - V 0..1 is the bright top rail
     * - V 15..16 is the very dark bottom rail
     *
     * Static Tank faces keep that baked directional shading. Dynamic
     * concave/stepped correction pieces instead reuse the bright source strips
     * and mirror them geometrically when they belong on the opposite edge.
     */
    private static final float CORRECTION_VERTICAL_U0 = 0.0f;
    private static final float CORRECTION_VERTICAL_U1 = 3.0f * PIXEL;
    private static final float CORRECTION_HORIZONTAL_V0 = 0.0f;
    private static final float CORRECTION_HORIZONTAL_V1 = PIXEL;

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

        /*
         * Always run the complete visual boundary overlay for Controller-owned
         * vessels.
         *
         * The previous v9 rectangular-prism shortcut was too aggressive:
         * although the six adjacency properties describe a rectangular Tank
         * volume, the baked two-sided frame planes can still leave tiny
         * inside-facing junction gaps. One Controller renderer is cheap enough
         * to own these visual joins for every vessel shape, and unlike the old
         * architecture this does NOT create a renderer/BlockEntity per Tank.
         */

        BlockPos controllerPos = controller.getBlockPos();
        Level level = controller.getLevel();

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
                        level,
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
                        level,
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
            Level level,
            int fallbackPackedLight,
            int packedOverlay
    ) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (structure.contains(tankPos.relative(face))) {
                continue;
            }

            /*
             * The correction quad lies on an exposed Tank face. Sample the
             * neighboring air cell, not the Tank block itself. Sampling the
             * occupied Tank position (v7) could return a heavily occluded light
             * value and make 1-pixel/inner correction pieces look absent.
             */
            int packedLight =
                    level != null
                            ? LevelRenderer.getLightColor(
                                    level,
                                    tankPos.relative(face)
                            )
                            : fallbackPackedLight;

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
             * Color/texture rule for generated inner frames:
             *
             * The source texture intentionally shades its right and bottom
             * perimeter almost black. That looks good as baked exterior
             * directional shading, but correction-only inner rails looked like
             * black geometry. Every correction below therefore samples the
             * bright metallic left/top source strip. Opposite edges mirror that
             * bright strip instead of sampling the dark source edge.
             */

            /*
             * For a non-rectangular vessel, reproduce the historical renderer's
             * complete exposed-face boundary. Some of these quads intentionally
             * overlay a static frame by 0.001 block. That is preferable to
             * splitting edge ownership across two topology systems, which was
             * the source of the missing inner lines/pixels.
             */
            if (topBoundary) {
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
                        CORRECTION_HORIZONTAL_V0,
                        CORRECTION_HORIZONTAL_V1,
                        packedLight,
                        packedOverlay
                );
            }

            if (bottomBoundary) {
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
                        CORRECTION_HORIZONTAL_V0,
                        CORRECTION_HORIZONTAL_V1,
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
            ) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        0.0f,
                        SIDE_MARKER_WIDTH,
                        verticalMinY,
                        verticalMaxY,
                        CORRECTION_VERTICAL_U0,
                        CORRECTION_VERTICAL_U1,
                        verticalMinV,
                        verticalMaxV,
                        packedLight,
                        packedOverlay
                );
            }

            if (
                    rightBoundary
                            && verticalMaxY > verticalMinY
            ) {
                renderSideRect(
                        face,
                        consumer,
                        pose,
                        SIDE_MARKER_RIGHT_MIN,
                        1.0f,
                        verticalMinY,
                        verticalMaxY,
                        CORRECTION_VERTICAL_U1,
                        CORRECTION_VERTICAL_U0,
                        verticalMinV,
                        verticalMaxV,
                        packedLight,
                        packedOverlay
                );
            }

            /*
             * Restore the exact one-/two-pixel join pieces from the original
             * Tank side renderer. The broad 3-pixel UV strip reproduces the
             * normal marker rows, but these seam pixels live specifically on
             * the top/bottom block boundary when the exposed side continues
             * vertically and therefore need their own geometry.
             */
            if (leftBoundary) {
                renderSideMarkerJoin(
                        tankPos,
                        face,
                        true,
                        structure,
                        consumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }

            if (rightBoundary) {
                renderSideMarkerJoin(
                        tankPos,
                        face,
                        false,
                        structure,
                        consumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }
        }
    }

    private static void renderSideMarkerJoin(
            BlockPos tankPos,
            Direction face,
            boolean leftSide,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        boolean continuesAbove =
                hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.UP,
                        structure
                );

        boolean continuesBelow =
                hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.DOWN,
                        structure
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
                            : 2.0f * PIXEL;

            float maxU =
                    leftSide
                            ? 2.0f * PIXEL
                            : PIXEL;

            renderSideRect(
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
                    packedOverlay
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
                            : 3.0f * PIXEL;

            float maxU =
                    leftSide
                            ? 3.0f * PIXEL
                            : PIXEL;

            renderSideRect(
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
                    packedOverlay
            );
        }
    }

    private static void renderHorizontalCorrections(
            BlockPos tankPos,
            Set<BlockPos> structure,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Level level,
            int fallbackPackedLight,
            int packedOverlay
    ) {
        if (!structure.contains(tankPos.above())) {
            int upPackedLight =
                    level != null
                            ? LevelRenderer.getLightColor(
                                    level,
                                    tankPos.above()
                            )
                            : fallbackPackedLight;

            renderHorizontalFaceCorrections(
                    tankPos,
                    Direction.UP,
                    structure,
                    consumer,
                    pose,
                    upPackedLight,
                    packedOverlay
            );
        }

        if (!structure.contains(tankPos.below())) {
            int downPackedLight =
                    level != null
                            ? LevelRenderer.getLightColor(
                                    level,
                                    tankPos.below()
                            )
                            : fallbackPackedLight;

            renderHorizontalFaceCorrections(
                    tankPos,
                    Direction.DOWN,
                    structure,
                    consumer,
                    pose,
                    downPackedLight,
                    packedOverlay
            );
        } else {
            /*
             * Raised-footprint caps sit around the base of this Tank. The
             * raised Tank's upper air cell is a stable exposed lighting sample
             * and avoids the zero/near-zero occupied-block sample from v7.
             */
            int raisedPackedLight =
                    level != null
                            ? LevelRenderer.getLightColor(
                                    level,
                                    tankPos.above()
                            )
                            : fallbackPackedLight;

            renderRaisedFootprintCornerCaps(
                    tankPos,
                    structure,
                    consumer,
                    pose,
                    raisedPackedLight,
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

        boolean westBoundary =
                !hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.WEST,
                        structure
                );

        boolean eastBoundary =
                !hasAdjacentExposedFace(
                        tankPos,
                        face,
                        Direction.EAST,
                        structure
                );

        /*
         * NORTH/SOUTH own the horizontal corner pixels. WEST/EAST correction
         * strips are trimmed around any north/south perimeter, exactly like
         * the historical dynamic renderer. This keeps dynamic stepped-frame
         * corrections from becoming another source of coplanar corner quads.
         */
        renderHorizontalBoundary(
                face,
                Direction.NORTH,
                northBoundary,
                northBoundary,
                southBoundary,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundary(
                face,
                Direction.SOUTH,
                southBoundary,
                northBoundary,
                southBoundary,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundary(
                face,
                Direction.WEST,
                westBoundary,
                northBoundary,
                southBoundary,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderHorizontalBoundary(
                face,
                Direction.EAST,
                eastBoundary,
                northBoundary,
                southBoundary,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        /*
         * Guarantee the physical 1x1 junction wherever two ordinary perimeter
         * strips meet. NORTH/SOUTH still own the main corner texture, but this
         * tiny cap sits an additional epsilon outward so the inside-facing
         * reverse view cannot expose a one-pixel crack between independently
         * baked strips.
         */
        renderBoundaryJunctionCap(
                face,
                northBoundary && westBoundary,
                0.0f,
                PIXEL,
                0.0f,
                PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderBoundaryJunctionCap(
                face,
                northBoundary && eastBoundary,
                1.0f - PIXEL,
                1.0f,
                0.0f,
                PIXEL,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderBoundaryJunctionCap(
                face,
                southBoundary && westBoundary,
                0.0f,
                PIXEL,
                1.0f - PIXEL,
                1.0f,
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderBoundaryJunctionCap(
                face,
                southBoundary && eastBoundary,
                1.0f - PIXEL,
                1.0f,
                1.0f - PIXEL,
                1.0f,
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

    private static void renderHorizontalBoundary(
            Direction face,
            Direction edgeDirection,
            boolean shouldRender,
            boolean northBoundary,
            boolean southBoundary,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (!shouldRender) {
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

    private static void renderBoundaryJunctionCap(
            Direction face,
            boolean shouldRender,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        if (!shouldRender) {
            return;
        }

        float extraOffset = HORIZONTAL_CORRECTION_OFFSET + 0.00020f;
        float y =
                face == Direction.UP
                        ? 1.0f + extraOffset
                        : -extraOffset;

        if (face == Direction.UP) {
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
        } else {
            renderQuad(
                    consumer,
                    pose,
                    minX, y, maxZ,
                    minX, y, minZ,
                    maxX, y, minZ,
                    maxX, y, maxZ,
                    CORNER_CAP_U0, CORNER_CAP_V1,
                    CORNER_CAP_U0, CORNER_CAP_V0,
                    CORNER_CAP_U1, CORNER_CAP_V0,
                    CORNER_CAP_U1, CORNER_CAP_V1,
                    0.0f, -1.0f, 0.0f,
                    packedLight,
                    packedOverlay
            );
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
        /*
         * Correction faces are displaced outward to stay in front of the baked
         * model and avoid z-fighting.
         *
         * A displaced side plane must also grow to the displaced planes that
         * meet it. Otherwise:
         *
         *   side plane:       x/z = +/- 0.001
         *   top plane:        y   = 1.001
         *   original extent:  y   <= 1.000
         *
         * leaves a real 0.001-block crack along the inside corner. That crack
         * was the "missing frame line / missing pixel" visible from oblique
         * angles in v5-v10.
         *
         * Expand only coordinates that already touch a block boundary. UVs are
         * deliberately unchanged; stretching by 0.001 block is visually
         * negligible, while the physical frame surfaces now overlap and become
         * watertight.
         */
        float expandedMinHorizontal =
                minHorizontal <= 0.0f
                        ? -SIDE_CORRECTION_OFFSET
                        : minHorizontal;

        float expandedMaxHorizontal =
                maxHorizontal >= 1.0f
                        ? 1.0f + SIDE_CORRECTION_OFFSET
                        : maxHorizontal;

        float expandedMinY =
                minY <= 0.0f
                        ? -HORIZONTAL_CORRECTION_OFFSET
                        : minY;

        float expandedMaxY =
                maxY >= 1.0f
                        ? 1.0f + HORIZONTAL_CORRECTION_OFFSET
                        : maxY;

        FoundryTankCasingQuads.renderSideRect(
                face,
                consumer,
                pose,
                expandedMinHorizontal,
                expandedMaxHorizontal,
                expandedMinY,
                expandedMaxY,
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
        /*
         * Match the side-frame expansion above. A top/bottom correction plane
         * displaced to y +/- 0.001 must extend to x/z +/- 0.001 wherever the
         * strip reaches a physical block edge. This makes horizontal and side
         * frame surfaces overlap at the exact displaced corner instead of
         * ending on two parallel-but-separated block boundaries.
         */
        float expandedMinX =
                minX <= 0.0f
                        ? -SIDE_CORRECTION_OFFSET
                        : minX;

        float expandedMaxX =
                maxX >= 1.0f
                        ? 1.0f + SIDE_CORRECTION_OFFSET
                        : maxX;

        float expandedMinZ =
                minZ <= 0.0f
                        ? -SIDE_CORRECTION_OFFSET
                        : minZ;

        float expandedMaxZ =
                maxZ >= 1.0f
                        ? 1.0f + SIDE_CORRECTION_OFFSET
                        : maxZ;

        float y =
                face == Direction.UP
                        ? 1.0f + HORIZONTAL_CORRECTION_OFFSET
                        : -HORIZONTAL_CORRECTION_OFFSET;

        if (face == Direction.UP) {
            FoundryTankCasingQuads.renderCasingQuad(
                    consumer,
                    pose,
                    expandedMinX, y, expandedMinZ,
                    expandedMinX, y, expandedMaxZ,
                    expandedMaxX, y, expandedMaxZ,
                    expandedMaxX, y, expandedMinZ,
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
                    expandedMinX, y, expandedMaxZ,
                    expandedMinX, y, expandedMinZ,
                    expandedMaxX, y, expandedMinZ,
                    expandedMaxX, y, expandedMaxZ,
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
