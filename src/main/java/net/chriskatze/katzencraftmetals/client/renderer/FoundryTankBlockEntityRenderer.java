package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dynamically renders the complete visual Foundry Tank multiblock.
 *
 * The JSON model remains available for the inventory item and particles,
 * while placed Tanks use this renderer for:
 *
 * - connected exterior frame textures
 * - hidden internal Tank faces
 * - seamless molten-metal volumes
 * - Faucet entrance shadow overlays
 */
public class FoundryTankBlockEntityRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity> {

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

    private static final ResourceLocation FAUCET_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_faucet_overlay.png"
            );

    private static final ResourceLocation MOLTEN_IRON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/molten_iron.png"
            );

    private static final float PIXEL =
            1.0f / 16.0f;

    /*
     * The side texture only contains pixels in the first two and last two
     * columns, plus the top and bottom rows. Cropping these exact regions
     * lets neighboring Tank faces form one uninterrupted large frame.
     */
    private static final float SIDE_VERTICAL_FRAME_WIDTH =
            PIXEL;

    private static final float SIDE_HORIZONTAL_FRAME_HEIGHT =
            PIXEL;

    private static final float TOP_PERIMETER_WIDTH =
            PIXEL;

    /*
     * The connected top/bottom frame can form a concave corner where two
     * orthogonal Tank neighbors exist but the diagonal Tank is missing.
     *
     * A one-pixel square must be rendered there or the two perimeter runs
     * leave a visible hole. These UVs sample one guaranteed opaque pixel from
     * the top row of foundry_tank_top.png.
     */
    private static final float CORNER_CAP_U0 =
            7.0f * PIXEL;

    private static final float CORNER_CAP_U1 =
            8.0f * PIXEL;

    private static final float CORNER_CAP_V0 =
            0.0f;

    private static final float CORNER_CAP_V1 =
            PIXEL;

    /*
     * Marker rows copied from the supplied 16x16 reference texture.
     *
     * Each normal marker is:
     *
     *   ##
     *   #
     *
     * The permanent one-pixel side rail is rendered separately.
     */
    private static final int[] MARKER_ROWS = {
            3,
            7,
            11
    };

    private static final float FAUCET_OVERLAY_OFFSET =
            0.002f;

    private static final float LIQUID_INSET =
            0.12f / 16.0f;

    private static final float LIQUID_MAX_INSET =
            15.88f / 16.0f;

    private static final float LIQUID_EPSILON =
            0.0001f;

    private static final float RISE_ANIMATION_TICKS =
            8.0f;

    private static final float DRAIN_ANIMATION_TICKS =
            FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    private final Map<FoundryTankBlockEntity, TankRenderState> renderStates =
            new WeakHashMap<>();

    public FoundryTankBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            FoundryTankBlockEntity tank,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (tank.getLevel() == null) {
            return;
        }

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        renderTankCasing(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        float displayedMoltenAmount =
                getDisplayedMoltenAmount(
                        tank,
                        partialTick
                );

        if (displayedMoltenAmount > LIQUID_EPSILON) {
            renderMoltenMetal(
                    tank,
                    displayedMoltenAmount,
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay
            );
        }

        /*
         * Render translucent entrance shadows after the molten metal so the
         * shadow blends over the visible liquid instead of hiding it through
         * the depth buffer.
         */
        renderAttachedFaucetOverlays(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    // =========================
    // CONNECTED TANK CASING
    // =========================

    private static void renderTankCasing(
            FoundryTankBlockEntity tank,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer sideConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                SIDE_TEXTURE
                        )
                );

        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (FoundryTankVisualConnections.isSameComponent(
                    tank,
                    face
            )) {
                continue;
            }

            renderConnectedSideFrame(
                    tank,
                    face,
                    sideConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        VertexConsumer topConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                TOP_TEXTURE
                        )
                );

        if (!FoundryTankVisualConnections.isSameComponent(
                tank,
                Direction.UP
        )) {
            renderConnectedHorizontalFrame(
                    tank,
                    Direction.UP,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        if (!FoundryTankVisualConnections.isSameComponent(
                tank,
                Direction.DOWN
        )) {
            renderConnectedHorizontalFrame(
                    tank,
                    Direction.DOWN,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        } else {
            /*
             * A raised Tank can sit inside a wider lower footprint.
             *
             * Example:
             *
             *     [ upper Tank ]
             *   [ ][ ][ ]
             *   [ ][ ][ ]
             *   [ ][ ][ ]
             *
             * The top frames of the surrounding lower Tanks meet at the four
             * corners of the raised Tank. Those junctions need an explicit
             * 1x1 cap even though the lower diagonal Tank exists.
             */
            renderRaisedFootprintCornerCaps(
                    tank,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }


    // =========================
    // FAUCET OVERLAY
    // =========================

    private static void renderAttachedFaucetOverlays(
            FoundryTankBlockEntity tank,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer overlayConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                FAUCET_OVERLAY_TEXTURE
                        )
                );

        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (
                    !FoundryTankVisualConnections.isSameComponent(
                            tank,
                            face
                    )
                            && FoundryTankVisualConnections.hasAttachedFaucet(
                            tank,
                            face
                    )
            ) {
                renderFaucetOverlay(
                        face,
                        overlayConsumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }
        }
    }

    private static void renderConnectedSideFrame(
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
            renderSideRect(
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
            renderSideRect(
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
            renderSideRect(
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
            renderSideRect(
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
            renderSideRect(
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
            renderSideRect(
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

            renderSideRect(
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

            renderSideRect(
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
                    packedOverlay,
                    0.0f
            );
        }
    }

    private static void renderConnectedHorizontalFrame(
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
            renderHorizontalRect(
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
            renderHorizontalRect(
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
            renderHorizontalRect(
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
            renderHorizontalRect(
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
            renderHorizontalRect(
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

    /**
     * Fills the four possible one-pixel gaps around the base of a Tank that
     * rises above a wider lower layer.
     *
     * This is a different case from a normal concave footprint corner:
     *
     * - the lower diagonal Tank exists
     * - the upper Tank occupies the center
     * - two exposed top-frame runs from the lower layer meet at the upper
     *   Tank's corner
     *
     * The cap is rendered on the exact shared Y plane. The Z-offset render
     * type gives it priority without moving the geometry inward or outward.
     */
    /**
     * Fills the four possible one-pixel gaps around the base of a Tank that
     * rises above a wider lower layer.
     *
     * Important geometry detail:
     *
     * The missing square is not inside the raised Tank's footprint. It is in
     * the diagonal lower Tank immediately outside each raised corner.
     *
     * For a north-west corner, for example, the required local coordinates
     * are:
     *
     *     X = -1 pixel .. 0
     *     Z = -1 pixel .. 0
     *
     * The previous implementation incorrectly used 0 .. +1 pixel, which
     * placed the cap inside the raised Tank and therefore could not fill the
     * visible hole.
     */
    private static void renderRaisedFootprintCornerCaps(
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

    private static void renderFaucetOverlay(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderSideRect(
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
                packedOverlay,
                FAUCET_OVERLAY_OFFSET
        );
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

    // =========================
    // SEAMLESS MOLTEN METAL
    // =========================

    private void renderMoltenMetal(
            FoundryTankBlockEntity tank,
            float displayedMoltenAmount,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        LiquidGeometry geometry =
                createLiquidGeometry(
                        tank,
                        displayedMoltenAmount,
                        partialTick
                );

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        tank.getLevel().getGameTime()
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                MOLTEN_IRON_TEXTURE
                        )
                );

        float frameMinV =
                animationFrame.minV();

        float frameMaxV =
                animationFrame.maxV();

        if (geometry.renderTop()) {
            renderLiquidHorizontalFace(
                    Direction.UP,
                    consumer,
                    pose,
                    geometry.minX(),
                    geometry.maxX(),
                    geometry.surfaceY(),
                    geometry.minZ(),
                    geometry.maxZ(),
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        if (geometry.renderBottom()) {
            renderLiquidHorizontalFace(
                    Direction.DOWN,
                    consumer,
                    pose,
                    geometry.minX(),
                    geometry.maxX(),
                    geometry.minY(),
                    geometry.minZ(),
                    geometry.maxZ(),
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            renderLiquidSide(
                    tank,
                    side,
                    geometry,
                    partialTick,
                    consumer,
                    pose,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private LiquidGeometry createLiquidGeometry(
            FoundryTankBlockEntity tank,
            float displayedMoltenAmount,
            float partialTick
    ) {
        FoundryTankBlockEntity tankBelow =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        Direction.DOWN
                );

        FoundryTankBlockEntity tankAbove =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        Direction.UP
                );

        float belowAmount =
                tankBelow != null
                        ? getDisplayedMoltenAmount(
                        tankBelow,
                        partialTick
                )
                        : 0.0f;

        float aboveAmount =
                tankAbove != null
                        ? getDisplayedMoltenAmount(
                        tankAbove,
                        partialTick
                )
                        : 0.0f;

        boolean liquidContinuesBelow =
                tankBelow != null
                        && belowAmount > LIQUID_EPSILON;

        boolean liquidContinuesAbove =
                tankAbove != null
                        && aboveAmount > LIQUID_EPSILON;

        float minX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.WEST
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.EAST
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        float minZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.NORTH
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.SOUTH
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        float minY =
                liquidContinuesBelow
                        ? 0.0f
                        : LIQUID_INSET;

        float fillPercentage =
                Mth.clamp(
                        displayedMoltenAmount
                                / FoundryTankBlockEntity.CAPACITY,
                        0.0f,
                        1.0f
                );

        float surfaceY =
                liquidContinuesAbove
                        ? 1.0f
                        : Mth.lerp(
                        fillPercentage,
                        minY,
                        LIQUID_MAX_INSET
                );

        return new LiquidGeometry(
                minX,
                maxX,
                minY,
                surfaceY,
                minZ,
                maxZ,
                !liquidContinuesAbove,
                !liquidContinuesBelow
        );
    }

    private void renderLiquidSide(
            FoundryTankBlockEntity tank,
            Direction side,
            LiquidGeometry geometry,
            float partialTick,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        FoundryTankBlockEntity neighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        side
                );

        if (neighbor == null) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    geometry.minY(),
                    geometry.surfaceY(),
                    false,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );

            return;
        }

        float neighborAmount =
                getDisplayedMoltenAmount(
                        neighbor,
                        partialTick
                );

        if (neighborAmount <= LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    geometry.minY(),
                    geometry.surfaceY(),
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );

            return;
        }

        LiquidGeometry neighborGeometry =
                createLiquidGeometry(
                        neighbor,
                        neighborAmount,
                        partialTick
                );

        float currentMin =
                geometry.minY();

        float currentMax =
                geometry.surfaceY();

        float neighborMin =
                neighborGeometry.minY();

        float neighborMax =
                neighborGeometry.surfaceY();

        float lowerSegmentMax =
                Math.min(
                        currentMax,
                        neighborMin
                );

        if (lowerSegmentMax - currentMin > LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    currentMin,
                    lowerSegmentMax,
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        float upperSegmentMin =
                Math.max(
                        currentMin,
                        neighborMax
                );

        if (currentMax - upperSegmentMin > LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    upperSegmentMin,
                    currentMax,
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private static void renderLiquidSideSegment(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            LiquidGeometry geometry,
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

        switch (side) {
            case NORTH -> renderLiquidQuad(
                    consumer,
                    pose,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.maxX(),
                    minY,
                    coordinate,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    1.0f - minY,
                    geometry.maxX(),
                    1.0f - minY,
                    geometry.maxX(),
                    1.0f - maxY,
                    geometry.minX(),
                    1.0f - maxY,
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
                    minY,
                    coordinate,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    1.0f - geometry.maxX(),
                    1.0f - minY,
                    1.0f - geometry.minX(),
                    1.0f - minY,
                    1.0f - geometry.minX(),
                    1.0f - maxY,
                    1.0f - geometry.maxX(),
                    1.0f - maxY,
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
                    minY,
                    geometry.maxZ(),
                    coordinate,
                    minY,
                    geometry.minZ(),
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    1.0f - geometry.maxZ(),
                    1.0f - minY,
                    1.0f - geometry.minZ(),
                    1.0f - minY,
                    1.0f - geometry.minZ(),
                    1.0f - maxY,
                    1.0f - geometry.maxZ(),
                    1.0f - maxY,
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
                    minY,
                    geometry.minZ(),
                    coordinate,
                    minY,
                    geometry.maxZ(),
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    geometry.minZ(),
                    1.0f - minY,
                    geometry.maxZ(),
                    1.0f - minY,
                    geometry.maxZ(),
                    1.0f - maxY,
                    geometry.minZ(),
                    1.0f - maxY,
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

    private static void renderLiquidHorizontalFace(
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

    // =========================
    // SMOOTH LOCAL FILL
    // =========================

    private float getDisplayedMoltenAmount(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        float targetAmount =
                tank.getLocalVisualMoltenAmount();

        TankRenderState renderState =
                renderStates.computeIfAbsent(
                        tank,
                        ignored -> new TankRenderState(
                                targetAmount,
                                currentRenderTime
                        )
                );

        if (
                Math.abs(
                        targetAmount
                                - renderState.lastTargetAmount
                ) > 0.00001f
        ) {
            renderState.transitionStartAmount =
                    renderState.displayedAmount;

            renderState.transitionTargetAmount =
                    targetAmount;

            renderState.transitionStartTime =
                    currentRenderTime;

            renderState.transitionDuration =
                    targetAmount
                            > renderState.lastTargetAmount
                            ? RISE_ANIMATION_TICKS
                            : DRAIN_ANIMATION_TICKS;

            renderState.lastTargetAmount =
                    targetAmount;
        }

        float progress =
                renderState.transitionDuration <= 0.0f
                        ? 1.0f
                        : Mth.clamp(
                        (float) (
                                (
                                        currentRenderTime
                                                - renderState.transitionStartTime
                                )
                                        / renderState.transitionDuration
                        ),
                        0.0f,
                        1.0f
                );

        renderState.displayedAmount =
                Mth.lerp(
                        progress,
                        renderState.transitionStartAmount,
                        renderState.transitionTargetAmount
                );

        if (progress >= 1.0f) {
            renderState.displayedAmount =
                    renderState.transitionTargetAmount;
        }

        return renderState.displayedAmount;
    }

    // =========================
    // CASING QUAD HELPERS
    // =========================

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

    private static void renderCasingQuad(
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

    // =========================
    // LIQUID QUAD HELPERS
    // =========================

    private static void renderLiquidQuad(
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

    private record LiquidGeometry(
            float minX,
            float maxX,
            float minY,
            float surfaceY,
            float minZ,
            float maxZ,
            boolean renderTop,
            boolean renderBottom
    ) {
    }

    private static final class TankRenderState {

        private float displayedAmount;
        private float lastTargetAmount;

        private float transitionStartAmount;
        private float transitionTargetAmount;

        private double transitionStartTime;
        private float transitionDuration;

        private TankRenderState(
                float initialAmount,
                double currentRenderTime
        ) {
            this.displayedAmount =
                    initialAmount;

            this.lastTargetAmount =
                    initialAmount;

            this.transitionStartAmount =
                    initialAmount;

            this.transitionTargetAmount =
                    initialAmount;

            this.transitionStartTime =
                    currentRenderTime;

            this.transitionDuration =
                    1.0f;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        return true;
    }
}
