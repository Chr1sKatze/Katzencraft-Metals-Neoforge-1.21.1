package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

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
     *
     * From above this is rendered as a fully dark top face.
     * From below this is rendered as a mostly opaque dark underside face with
     * only slight transparency.
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

    private static final float OPENING_UNDERSIDE_Y =
            1.001f;

    private static final float RIM_UNDERSIDE_Y =
            1.002f;

    private static final float RIM_TOP_Y =
            1.0f + PIXEL + 0.003f;

    /*
     * The foundry_tank_top texture contains transparent interior pixels.
     * The rim top and bottom surfaces must not use their world-space UVs
     * directly or they can sample transparent pixels and disappear. Instead,
     * sample from a known opaque corner strip of the tank top texture.
     */
    private static final float SOLID_U0 =
            0.0f;

    private static final float SOLID_U1 =
            PIXEL;

    private static final float SOLID_V0 =
            0.0f;

    private static final float SOLID_V1 =
            PIXEL;

    private static final int FULL_TOP_COLOR =
            0xFFFFFFFF;

    private static final int RIM_TOP_COLOR =
            0xFFFFFFFF;

    private static final int RIM_BOTTOM_COLOR =
            0xFFFFFFFF;

    private static final int RIM_OUTER_SIDE_COLOR =
            0xFFB8B8B8;

    private static final int RIM_INNER_SIDE_COLOR =
            0xFF5C5C5C;

    /*
     * Top is fully dark again.
     * Bottom is only slightly transparent: alpha CC is about 80% opaque.
     */
    private static final int OPENING_TOP_COLOR =
            0xFF050505;

    private static final int OPENING_BOTTOM_COLOR =
            0xCC050505;

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

        boolean cameraAboveHatch =
                isCameraAboveHatch(tank);

        /*
         * Render all solid/no-cull geometry first.
         *
         * Do not switch render buffers and then continue writing to this
         * consumer. Buffer switching can invalidate the old builder and cause
         * "IllegalStateException: Not building!".
         */
        VertexConsumer solidConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                TOP_TEXTURE
                        )
                );

        renderFullTopCap(
                solidConsumer,
                pose,
                packedLight,
                packedOverlay
        );

        if (cameraAboveHatch) {
            renderDarkOpeningTop(
                    solidConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        renderRaisedRim(
                solidConsumer,
                pose,
                packedLight,
                packedOverlay
        );

        /*
         * Render the translucent underside last, after all solid hatch geometry.
         */
        if (!cameraAboveHatch) {
            VertexConsumer translucentConsumer =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(
                                    TOP_TEXTURE
                            )
                    );

            renderDarkOpeningBottomSlightlyTransparent(
                    translucentConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static boolean isCameraAboveHatch(
            FoundryTankBlockEntity tank
    ) {
        if (tank.getLevel() == null) {
            return true;
        }

        Vec3 cameraPosition =
                Minecraft.getInstance()
                        .gameRenderer
                        .getMainCamera()
                        .getPosition();

        return cameraPosition.y
                >= tank.getBlockPos()
                .getY() + 1.0;
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

    private static void renderDarkOpeningTop(
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
                OPENING_TOP_COLOR,
                packedLight,
                packedOverlay
        );
    }

    private static void renderDarkOpeningBottomSlightlyTransparent(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderBottomRectSolidUv(
                consumer,
                pose,
                OPENING_MIN,
                OPENING_MAX,
                OPENING_MIN,
                OPENING_MAX,
                OPENING_UNDERSIDE_Y,
                OPENING_BOTTOM_COLOR,
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
        renderTopRimSurfaces(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        /*
         * Bottom face of the raised rim, visible when looking through the glass
         * sides from inside/below the Tank.
         */
        renderBottomRimSurfaces(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );

        renderRimSideWalls(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderTopRimSurfaces(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
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
    }

    private static void renderBottomRimSurfaces(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderBottomRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_OUTER_MIN,
                RIM_INNER_MIN,
                RIM_UNDERSIDE_Y,
                RIM_BOTTOM_COLOR,
                packedLight,
                packedOverlay
        );

        renderBottomRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_OUTER_MAX,
                RIM_INNER_MAX,
                RIM_OUTER_MAX,
                RIM_UNDERSIDE_Y,
                RIM_BOTTOM_COLOR,
                packedLight,
                packedOverlay
        );

        renderBottomRectSolidUv(
                consumer,
                pose,
                RIM_OUTER_MIN,
                RIM_INNER_MIN,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_UNDERSIDE_Y,
                RIM_BOTTOM_COLOR,
                packedLight,
                packedOverlay
        );

        renderBottomRectSolidUv(
                consumer,
                pose,
                RIM_INNER_MAX,
                RIM_OUTER_MAX,
                RIM_INNER_MIN,
                RIM_INNER_MAX,
                RIM_UNDERSIDE_Y,
                RIM_BOTTOM_COLOR,
                packedLight,
                packedOverlay
        );
    }

    private static void renderRimSideWalls(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
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
                SOLID_U0,
                SOLID_V0,
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
                SOLID_U0,
                SOLID_V1,
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
                SOLID_U1,
                SOLID_V1,
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
                SOLID_U1,
                SOLID_V0,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );
    }

    private static void renderBottomRectSolidUv(
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
                maxX,
                y,
                minZ,
                SOLID_U1,
                SOLID_V0,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                -1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                maxZ,
                SOLID_U1,
                SOLID_V1,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                -1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                minX,
                y,
                maxZ,
                SOLID_U0,
                SOLID_V1,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                -1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                minX,
                y,
                minZ,
                SOLID_U0,
                SOLID_V0,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                -1.0f,
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
