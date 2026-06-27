package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class FoundryFaucetBlockEntityRenderer
        implements BlockEntityRenderer<FoundryFaucetBlockEntity> {

    /*
     * Use the same still molten-iron sheet as the Tank and Cauldron.
     *
     * The shared MoltenIronAnimation helper keeps every rendered
     * surface on the exact same vanilla-style frame and speed.
     */
    private static final ResourceLocation MOLTEN_IRON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/molten_iron.png"
            );

    /*
     * Interior coordinates of the open Faucet channel.
     *
     * The unrotated model points north.
     */
    private static final float CHANNEL_MIN_X = 6.05f / 16.0f;
    private static final float CHANNEL_MAX_X = 9.95f / 16.0f;

    private static final float CHANNEL_MIN_Z = 10.0f / 16.0f;
    private static final float CHANNEL_MAX_Z = 15.95f / 16.0f;

    /*
     * A one-pixel-deep layer of molten metal inside the channel.
     */
    private static final float CHANNEL_BOTTOM_Y = 6.05f / 16.0f;
    private static final float CHANNEL_TOP_Y = 7.05f / 16.0f;

    /*
     * The falling stream has the same width as the liquid channel.
     */
    private static final float STREAM_MIN_X = CHANNEL_MIN_X;
    private static final float STREAM_MAX_X = CHANNEL_MAX_X;

    /*
     * The stream begins exactly at the channel edge.
     */
    private static final float STREAM_MAX_Z =
            CHANNEL_MIN_Z;

    /*
     * The stream extends two pixels outward from the Faucet.
     */
    private static final float STREAM_MIN_Z =
            STREAM_MAX_Z - 2.0f / 16.0f;

    /*
     * The inner edge connects to the horizontal channel liquid.
     *
     * The outer edge is one pixel lower, creating the sloped
     * transition into the falling stream.
     */
    private static final float STREAM_INNER_TOP_Y =
            CHANNEL_TOP_Y;

    private static final float STREAM_OUTER_TOP_Y =
            CHANNEL_TOP_Y - 1.0f / 16.0f;

    /*
     * UV dimensions based on the physical size of each face.
     *
     * A value of 1.0 represents the width or height of one
     * complete Minecraft block.
     */
    private static final float CHANNEL_WIDTH_UV =
            CHANNEL_MAX_X - CHANNEL_MIN_X;

    private static final float CHANNEL_DEPTH_UV =
            CHANNEL_MAX_Z - CHANNEL_MIN_Z;

    private static final float CHANNEL_HEIGHT_UV =
            CHANNEL_TOP_Y - CHANNEL_BOTTOM_Y;

    private static final float STREAM_WIDTH_UV =
            STREAM_MAX_X - STREAM_MIN_X;

    private static final float STREAM_DEPTH_UV =
            STREAM_MAX_Z - STREAM_MIN_Z;

    /*
     * These must match the interior levels used by the
     * CastingCauldronBlockEntityRenderer.
     */
    private static final float CAULDRON_MIN_Y = 4.05f / 16.0f;
    private static final float CAULDRON_MAX_Y = 14.25f / 16.0f;

    private static final float STREAM_ANIMATION_DURATION_TICKS =
            FoundryFaucetBlockEntity.STREAM_ANIMATION_STEPS
                    * FoundryFaucetBlockEntity.STREAM_ANIMATION_INTERVAL;

    private final Map<FoundryFaucetBlockEntity, StreamRenderState>
            streamRenderStates =
            new WeakHashMap<>();

    public FoundryFaucetBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            FoundryFaucetBlockEntity faucet,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (faucet.getLevel() == null) {
            return;
        }

        float streamProgress =
                getDisplayedStreamProgress(
                        faucet,
                        partialTick
                );

        boolean pouring =
                faucet.isPouring();

        if (
                !pouring
                        && streamProgress <= 0.0001f
        ) {
            streamRenderStates.remove(faucet);
            return;
        }

        BlockEntity blockEntityBelow =
                faucet.getLevel().getBlockEntity(
                        faucet.getBlockPos().below()
                );

        if (!(blockEntityBelow instanceof CastingCauldronBlockEntity cauldron)) {
            return;
        }

        float displayedMoltenAmount =
                CastingCauldronFillSmoother.getDisplayedMoltenAmount(
                        cauldron,
                        partialTick
                );

        float fillPercentage = Mth.clamp(
                displayedMoltenAmount
                        / CastingCauldronBlockEntity.REQUIRED_MOLTEN_AMOUNT,
                0.0f,
                1.0f
        );

        float cauldronSurfaceY =
                -1.0f + Mth.lerp(
                        fillPercentage,
                        CAULDRON_MIN_Y,
                        CAULDRON_MAX_Y
                );

        float streamBottomY;
        float streamInnerTopY;
        float streamOuterTopY;

        if (pouring) {
            streamBottomY =
                    Mth.lerp(
                            streamProgress,
                            STREAM_OUTER_TOP_Y,
                            cauldronSurfaceY
                    );

            streamInnerTopY =
                    STREAM_INNER_TOP_Y;

            streamOuterTopY =
                    STREAM_OUTER_TOP_Y;
        } else {
            streamBottomY =
                    cauldronSurfaceY;

            streamInnerTopY =
                    Mth.lerp(
                            streamProgress,
                            streamBottomY,
                            STREAM_INNER_TOP_Y
                    );

            streamOuterTopY =
                    Mth.lerp(
                            streamProgress,
                            streamBottomY,
                            STREAM_OUTER_TOP_Y
                    );
        }

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        faucet.getLevel().getGameTime()
                );

        float frameMinV =
                animationFrame.minV();

        float frameMaxV =
                animationFrame.maxV();

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                MOLTEN_IRON_TEXTURE
                        )
                );

        poseStack.pushPose();

        rotateForFacing(
                poseStack,
                faucet.getBlockState().getValue(
                        FoundryFaucetBlock.FACING
                )
        );

        PoseStack.Pose pose =
                poseStack.last();

        renderMoltenPass(
                consumer,
                pose,
                pouring,
                streamBottomY,
                streamInnerTopY,
                streamOuterTopY,
                packedOverlay,
                frameMinV,
                frameMaxV,
                255
        );

        poseStack.popPose();
    }

    private float getDisplayedStreamProgress(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        double currentRenderTime =
                faucet.getLevel().getGameTime()
                        + partialTick;

        float initialProgress = Mth.clamp(
                (float) faucet.getStreamAnimationStep()
                        / FoundryFaucetBlockEntity.STREAM_ANIMATION_STEPS,
                0.0f,
                1.0f
        );

        StreamRenderState renderState =
                streamRenderStates.computeIfAbsent(
                        faucet,
                        ignored -> new StreamRenderState(
                                initialProgress,
                                currentRenderTime
                        )
                );

        double elapsedTicks =
                Math.max(
                        0.0,
                        currentRenderTime
                                - renderState.lastRenderTime
                );

        renderState.lastRenderTime =
                currentRenderTime;

        float targetProgress =
                faucet.isPouring()
                        ? 1.0f
                        : 0.0f;

        float maximumChange =
                (float) elapsedTicks
                        / STREAM_ANIMATION_DURATION_TICKS;

        if (targetProgress > renderState.displayedProgress) {
            renderState.displayedProgress =
                    Math.min(
                            targetProgress,
                            renderState.displayedProgress
                                    + maximumChange
                    );
        } else if (targetProgress < renderState.displayedProgress) {
            renderState.displayedProgress =
                    Math.max(
                            targetProgress,
                            renderState.displayedProgress
                                    - maximumChange
                    );
        }

        return Mth.clamp(
                renderState.displayedProgress,
                0.0f,
                1.0f
        );
    }

    private static void renderMoltenPass(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            boolean pouring,
            float streamBottomY,
            float streamInnerTopY,
            float streamOuterTopY,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        if (pouring) {
            renderChannelLiquid(
                    consumer,
                    pose,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }

        if (streamBottomY < streamOuterTopY) {
            renderStream(
                    consumer,
                    pose,
                    streamBottomY,
                    streamInnerTopY,
                    streamOuterTopY,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }
    }

    private static void rotateForFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        float rotationDegrees =
                switch (facing) {
                    case NORTH -> 0.0f;
                    case EAST -> 270.0f;
                    case SOUTH -> 180.0f;
                    case WEST -> 90.0f;
                    default -> 0.0f;
                };

        poseStack.translate(
                0.5f,
                0.0f,
                0.5f
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationDegrees
                )
        );

        poseStack.translate(
                -0.5f,
                0.0f,
                -0.5f
        );
    }

    private static void renderChannelLiquid(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        // =========================
        // TOP SURFACE
        // =========================

        addVertex(
                consumer,
                pose,
                CHANNEL_MIN_X,
                CHANNEL_TOP_Y,
                CHANNEL_MIN_Z,
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
                CHANNEL_MAX_Z,
                0.0f,
                CHANNEL_DEPTH_UV,
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
                CHANNEL_MAX_Z,
                CHANNEL_WIDTH_UV,
                CHANNEL_DEPTH_UV,
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
                CHANNEL_MIN_Z,
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

        /*
         * The four vertical faces use UV ranges that match their
         * physical dimensions instead of stretching a full frame
         * over every face.
         */

        // Front
        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MIN_X,
                CHANNEL_MIN_Z,
                CHANNEL_MAX_X,
                CHANNEL_MIN_Z,
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

        // Back
        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MAX_X,
                CHANNEL_MAX_Z,
                CHANNEL_MIN_X,
                CHANNEL_MAX_Z,
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

        // West
        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MIN_X,
                CHANNEL_MAX_Z,
                CHANNEL_MIN_X,
                CHANNEL_MIN_Z,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                CHANNEL_DEPTH_UV,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // East
        renderRepeatedVerticalFace(
                consumer,
                pose,
                CHANNEL_MAX_X,
                CHANNEL_MIN_Z,
                CHANNEL_MAX_X,
                CHANNEL_MAX_Z,
                CHANNEL_BOTTOM_Y,
                CHANNEL_TOP_Y,
                CHANNEL_DEPTH_UV,
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
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        float minX = STREAM_MIN_X;
        float maxX = STREAM_MAX_X;

        /*
         * minZ is the outer edge.
         * maxZ is the edge connected to the Faucet.
         */
        float minZ = STREAM_MIN_Z;
        float maxZ = STREAM_MAX_Z;

        /*
         * Render the long rectangular body in sections no taller
         * than one block. Each section repeats only the currently
         * selected animation frame, so the renderer never samples
         * into the next frame of the vertical texture sheet.
         */

        // Outer face
        renderRepeatedVerticalFace(
                consumer,
                pose,
                minX,
                minZ,
                maxX,
                minZ,
                bottomY,
                outerTopY,
                STREAM_WIDTH_UV,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // Inner face
        renderRepeatedVerticalFace(
                consumer,
                pose,
                maxX,
                maxZ,
                minX,
                maxZ,
                bottomY,
                outerTopY,
                STREAM_WIDTH_UV,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // West side
        renderRepeatedVerticalFace(
                consumer,
                pose,
                minX,
                maxZ,
                minX,
                minZ,
                bottomY,
                outerTopY,
                STREAM_DEPTH_UV,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // East side
        renderRepeatedVerticalFace(
                consumer,
                pose,
                maxX,
                minZ,
                maxX,
                maxZ,
                bottomY,
                outerTopY,
                STREAM_DEPTH_UV,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        /*
         * The inner edge is slightly higher than the outer edge.
         * This small wedge closes the stream and preserves the
         * rounded downward outlet shape.
         */
        float wedgeHeight =
                innerTopY - outerTopY;

        if (wedgeHeight > 0.00001f) {
            // Inner face of the wedge
            renderRepeatedVerticalFace(
                    consumer,
                    pose,
                    maxX,
                    maxZ,
                    minX,
                    maxZ,
                    outerTopY,
                    innerTopY,
                    STREAM_WIDTH_UV,
                    0.0f,
                    0.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );

            // West triangular side
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
                    STREAM_DEPTH_UV,
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
                    STREAM_DEPTH_UV,
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

            // East triangular side
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
                    STREAM_DEPTH_UV,
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
                    STREAM_DEPTH_UV,
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

        // =========================
        // SLOPED TOP
        // =========================

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
                STREAM_DEPTH_UV,
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
                STREAM_WIDTH_UV,
                STREAM_DEPTH_UV,
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
                STREAM_WIDTH_UV,
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

    /*
     * Draws a vertical face in one-block-high pieces.
     *
     * The UV height of each piece equals its model height. This
     * preserves the texture's pixel density and repeats the active
     * animation frame instead of stretching it along a long face.
     */
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
                .setColor(255, 255, 255, alpha)
                .setUv(
                        u,
                        Mth.lerp(
                                v,
                                frameMinV,
                                frameMaxV
                        )
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

    private static final class StreamRenderState {

        private float displayedProgress;
        private double lastRenderTime;

        private StreamRenderState(
                float displayedProgress,
                double lastRenderTime
        ) {
            this.displayedProgress =
                    displayedProgress;

            this.lastRenderTime =
                    lastRenderTime;
        }
    }

    /*
     * The stream extends into the block below the Faucet.
     * Rendering off-screen avoids it disappearing too early
     * at the edge of the camera view.
     */
    @Override
    public boolean shouldRenderOffScreen(
            FoundryFaucetBlockEntity faucet
    ) {
        return true;
    }
}