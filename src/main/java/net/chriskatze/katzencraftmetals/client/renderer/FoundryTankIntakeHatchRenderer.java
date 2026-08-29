package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.PIXEL;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_TEXTURE;

/**
 * Visual intake hatch for top Tanks toggled into ore-loading mode.
 *
 * The hatch is still only an illusion: internally the Tank stays closed molten
 * storage, but visually the top becomes a single-block cap with a raised
 * loading rim.
 */
final class FoundryTankIntakeHatchRenderer {

    /*
     * Full top cap.
     *
     * This deliberately covers the connected multiblock top framing for this
     * one open hatch Tank so it reads as a single physical loading block.
     */
    private static final float FULL_MIN =
            0.0f;

    private static final float FULL_MAX =
            1.0f;

    /*
     * Center dark opening: 6x6 pixels.
     */
    private static final float OPENING_MIN =
            5.0f * PIXEL;

    private static final float OPENING_MAX =
            11.0f * PIXEL;

    /*
     * Raised rim: 8x8 outside, 6x6 inside, 1 pixel tall.
     */
    private static final float RIM_OUTER_MIN =
            4.0f * PIXEL;

    private static final float RIM_OUTER_MAX =
            12.0f * PIXEL;

    private static final float RIM_INNER_MIN =
            OPENING_MIN;

    private static final float RIM_INNER_MAX =
            OPENING_MAX;

    private static final float BASE_Y =
            1.003f;

    private static final float OPENING_Y =
            1.006f;

    private static final float RIM_TOP_Y =
            1.0f + PIXEL + 0.003f;

    /*
     * The foundry_tank_top texture contains transparent interior pixels.
     * The rim top surfaces must not use their world-space UVs directly or they
     * can sample those transparent pixels and disappear. Instead, sample from
     * the known opaque top-left corner strip of the tank top texture.
     */
    private static final float RIM_U0 =
            0.0f;

    private static final float RIM_U1 =
            PIXEL;

    private static final float RIM_V0 =
            0.0f;

    private static final float RIM_V1 =
            PIXEL;

    private static final int FULL_TOP_COLOR =
            0xFFFFFFFF;

    private static final int RIM_TOP_COLOR =
            0xFFFFFFFF;

    private static final int RIM_OUTER_SIDE_COLOR =
            0xFFB8B8B8;

    private static final int RIM_INNER_SIDE_COLOR =
            0xFF5C5C5C;

    private static final int OPENING_COLOR =
            0xFF050505;

    private FoundryTankIntakeHatchRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (
                !tank.isIntakeHatchOpen()
                        || !tank.isTopTank()
        ) {
            return;
        }

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                TOP_TEXTURE
                        )
                );

        renderFullTopCap(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderDarkOpening(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRaisedRim(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderFullTopCap(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderTopRect(
                consumer,
                pose,
                FULL_MIN,
                FULL_MAX,
                FULL_MIN,
                FULL_MAX,
                BASE_Y,
                FULL_TOP_COLOR,
                packedLight,
                packedOverlay
        );
    }

    private static void renderDarkOpening(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderTopRectSolidUv(
                consumer,
                pose,
                OPENING_MIN,
                OPENING_MAX,
                OPENING_MIN,
                OPENING_MAX,
                OPENING_Y,
                OPENING_COLOR,
                packedLight,
                packedOverlay
        );
    }

    private static void renderRaisedRim(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        /*
         * Top face of the raised rim.
         *
         * These use fixed opaque UVs instead of world-space UVs because the
         * middle of foundry_tank_top.png can be transparent.
         */
        renderTopRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_OUTER_MIN,
                RIM_INNER_MIN,
                RIM_TOP_Y,
                RIM_TOP_COLOR,
                packedLight,
                packedOverlay
        );

        renderTopRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_INNER_MAX,
                RIM_OUTER_MAX,
                RIM_TOP_Y,
                RIM_TOP_COLOR,
                packedLight,
                packedOverlay
        );

        renderTopRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_INNER_MIN,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_TOP_Y,
                RIM_TOP_COLOR,
                packedLight,
                packedOverlay
        );

        renderTopRectSolidUv(
                consumer,
                pose,
                RIM_INNER_MAX,
                RIM_OUTER_MAX,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_TOP_Y,
                RIM_TOP_COLOR,
                packedLight,
                packedOverlay
        );

        /*
         * Outer side walls.
         */
        renderZWall(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_OUTER_MIN,
                BASE_Y,
                RIM_TOP_Y,
                RIM_OUTER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                -1.0f
        );

        renderZWall(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_OUTER_MAX,
                BASE_Y,
                RIM_TOP_Y,
                RIM_OUTER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                1.0f
        );

        renderXWall(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                BASE_Y,
                RIM_TOP_Y,
                RIM_OUTER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                -1.0f
        );

        renderXWall(
                consumer,
                pose,
                RIM_OUTER_MAX,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                BASE_Y,
                RIM_TOP_Y,
                RIM_OUTER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                1.0f
        );

        /*
         * Inner side walls toward the loading hole.
         */
        renderZWall(
                consumer,
                pose,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_INNER_MIN,
                OPENING_Y,
                RIM_TOP_Y,
                RIM_INNER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                1.0f
        );

        renderZWall(
                consumer,
                pose,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_INNER_MAX,
                OPENING_Y,
                RIM_TOP_Y,
                RIM_INNER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                -1.0f
        );

        renderXWall(
                consumer,
                pose,
                RIM_INNER_MIN,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                OPENING_Y,
                RIM_TOP_Y,
                RIM_INNER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                1.0f
        );

        renderXWall(
                consumer,
                pose,
                RIM_INNER_MAX,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                OPENING_Y,
                RIM_TOP_Y,
                RIM_INNER_SIDE_COLOR,
                packedLight,
                packedOverlay,
                -1.0f
        );
    }

    private static void renderTopRect(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float y,
            int color,
            int packedLight,
            int packedOverlay
    ) {
        addVertex(
                consumer,
                pose,
                minX,
                y,
                minZ,
                minX,
                minZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                minX,
                y,
                maxZ,
                minX,
                maxZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                maxZ,
                maxX,
                maxZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                minZ,
                maxX,
                minZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );
    }

    private static void renderTopRectSolidUv(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float y,
            int color,
            int packedLight,
            int packedOverlay
    ) {
        addVertex(
                consumer,
                pose,
                minX,
                y,
                minZ,
                RIM_U0,
                RIM_V0,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                minX,
                y,
                maxZ,
                RIM_U0,
                RIM_V1,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                maxZ,
                RIM_U1,
                RIM_V1,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                minZ,
                RIM_U1,
                RIM_V0,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );
    }

    private static void renderZWall(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float z,
            float minY,
            float maxY,
            int color,
            int packedLight,
            int packedOverlay,
            float normalZ
    ) {
        addVertex(
                consumer,
                pose,
                minX,
                minY,
                z,
                minX,
                minY,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                0.0f,
                normalZ
        );

        addVertex(
                consumer,
                pose,
                maxX,
                minY,
                z,
                maxX,
                minY,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                0.0f,
                normalZ
        );

        addVertex(
                consumer,
                pose,
                maxX,
                maxY,
                z,
                maxX,
                maxY,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                0.0f,
                normalZ
        );

        addVertex(
                consumer,
                pose,
                minX,
                maxY,
                z,
                minX,
                maxY,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                0.0f,
                normalZ
        );
    }

    private static void renderXWall(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float minZ,
            float maxZ,
            float minY,
            float maxY,
            int color,
            int packedLight,
            int packedOverlay,
            float normalX
    ) {
        addVertex(
                consumer,
                pose,
                x,
                minY,
                maxZ,
                maxZ,
                minY,
                color,
                packedLight,
                packedOverlay,
                normalX,
                0.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                x,
                minY,
                minZ,
                minZ,
                minY,
                color,
                packedLight,
                packedOverlay,
                normalX,
                0.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                x,
                maxY,
                minZ,
                minZ,
                maxY,
                color,
                packedLight,
                packedOverlay,
                normalX,
                0.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                x,
                maxY,
                maxZ,
                maxZ,
                maxY,
                color,
                packedLight,
                packedOverlay,
                normalX,
                0.0f,
                0.0f
        );
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color,
            int packedLight,
            int packedOverlay,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        color
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(
                        packedOverlay
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
