package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class FoundryFaucetBlockEntityRenderer
        implements BlockEntityRenderer<FoundryFaucetBlockEntity> {

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

    /*
     * The first quarter of the animation moves the molten iron
     * horizontally through the Faucet channel.
     *
     * The remaining three quarters extend or retract the vertical stream.
     *
     * With the current 16-tick total duration:
     *
     * horizontal movement = 4 ticks
     * vertical movement   = 12 ticks
     */
    private static final float HORIZONTAL_PHASE_SHARE =
            0.25f;

    /*
     * The shutdown stream begins breaking into separate full-width
     * pieces quite early.
     *
     * The pieces keep the normal stream width and depth. Only their
     * vertical lengths, gaps, and falling positions change.
     */
    private static final float DRIP_BREAKUP_START =
            0.18f;

    /*
     * Even when the Faucet is switched off before a vertical stream has
     * fully formed, the liquid remaining in the lip produces a short drip.
     */
    private static final float MINIMUM_EARLY_DRIP_HEIGHT =
            2.0f / 16.0f;

    /*
     * When molten metal is already present, let the drops enter it slightly
     * before disappearing. In an empty Cauldron they continue to the bottom.
     */
    private static final float MOLTEN_SURFACE_PENETRATION =
            1.0f / 16.0f;

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

        float animationProgress =
                updateAndGetAnimationProgress(
                        faucet,
                        partialTick
                );

        StreamRenderState renderState =
                streamRenderStates.get(faucet);

        DripStyle dripStyle =
                renderState != null
                        ? renderState.dripStyle
                        : DripStyle.HORIZONTAL_ONLY;

        boolean pouring =
                faucet.isPouring();

        float startupProgress =
                pouring
                        ? animationProgress
                        : 0.0f;

        float shutdownProgress =
                pouring
                        ? 0.0f
                        : animationProgress;

        float shutdownStartProgress =
                renderState != null
                        ? renderState.drainStartProgress
                        : 0.0f;

        /*
         * Startup and shutdown intentionally use the same physical
         * direction of travel.
         *
         * Startup:
         * 1. liquid moves from the Tank side toward the Faucet lip
         * 2. the vertical stream extends downward
         *
         * Shutdown:
         * 1. the remaining channel liquid drains outward toward the lip
         * 2. the detached vertical strand falls into the Cauldron
         *
         * Shutdown is therefore not the startup animation played backward.
         */
        float channelOuterZ;
        float channelInnerZ;
        float verticalProgress;
        float shutdownInitialVerticalProgress =
                0.0f;

        if (pouring) {
            float horizontalFlowProgress = Mth.clamp(
                    startupProgress
                            / HORIZONTAL_PHASE_SHARE,
                    0.0f,
                    1.0f
            );

            verticalProgress = Mth.clamp(
                    (
                            startupProgress
                                    - HORIZONTAL_PHASE_SHARE
                    )
                            / (
                            1.0f
                                    - HORIZONTAL_PHASE_SHARE
                    ),
                    0.0f,
                    1.0f
            );

            channelInnerZ =
                    CHANNEL_MAX_Z;

            channelOuterZ =
                    Mth.lerp(
                            horizontalFlowProgress,
                            CHANNEL_MAX_Z,
                            CHANNEL_MIN_Z
                    );
        } else {
            /*
             * Reconstruct the exact geometry that existed at the instant
             * the Faucet was switched off.
             *
             * A quick click-off during the first few startup ticks therefore
             * begins draining from that small partial channel instead of
             * jumping directly to the end of a full shutdown animation.
             */
            float initialHorizontalProgress = Mth.clamp(
                    shutdownStartProgress
                            / HORIZONTAL_PHASE_SHARE,
                    0.0f,
                    1.0f
            );

            shutdownInitialVerticalProgress = Mth.clamp(
                    (
                            shutdownStartProgress
                                    - HORIZONTAL_PHASE_SHARE
                    )
                            / (
                            1.0f
                                    - HORIZONTAL_PHASE_SHARE
                    ),
                    0.0f,
                    1.0f
            );

            float horizontalDrainProgress = Mth.clamp(
                    shutdownProgress
                            / HORIZONTAL_PHASE_SHARE,
                    0.0f,
                    1.0f
            );

            verticalProgress = Mth.clamp(
                    (
                            shutdownProgress
                                    - HORIZONTAL_PHASE_SHARE
                    )
                            / (
                            1.0f
                                    - HORIZONTAL_PHASE_SHARE
                    ),
                    0.0f,
                    1.0f
            );

            /*
             * The initial outer edge is wherever the startup animation
             * had actually reached.
             */
            channelOuterZ =
                    Mth.lerp(
                            initialHorizontalProgress,
                            CHANNEL_MAX_Z,
                            CHANNEL_MIN_Z
                    );

            /*
             * The rear cutoff edge then moves outward through only the
             * liquid that was truly present.
             */
            channelInnerZ =
                    Mth.lerp(
                            horizontalDrainProgress,
                            CHANNEL_MAX_Z,
                            channelOuterZ
                    );
        }

        if (
                !pouring
                        && shutdownProgress >= 0.9999f
        ) {
            /*
             * Wait until the server-side stream step also reaches zero
             * before discarding the state. This prevents a completed client
             * animation from being reconstructed for one frame.
             */
            if (faucet.getStreamAnimationStep() <= 0) {
                streamRenderStates.remove(faucet);
            }

            return;
        }

        FoundryFaucetBlockEntity.CauldronTarget cauldronTarget =
                FoundryFaucetBlockEntity.findCauldronTarget(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );

        if (cauldronTarget == null) {
            return;
        }

        CastingCauldronBlockEntity cauldron =
                cauldronTarget.cauldron();

        /*
         * The Faucet BlockEntity locks the selected metal when pouring starts.
         * Keep that identity through the complete shutdown and drip animation.
         * The Cauldron and attached Tank are only legacy/reload fallbacks.
         */
        ResourceLocation renderedMetalId =
                faucet.getPouringMetal();

        if (renderedMetalId == null) {
            renderedMetalId =
                    cauldron.getStoredMetal();
        }

        if (renderedMetalId == null) {
            Direction facing =
                    faucet.getBlockState().getValue(
                            FoundryFaucetBlock.FACING
                    );

            BlockPos sourceTankPos =
                    faucet.getBlockPos().relative(
                            facing.getOpposite()
                    );

            BlockEntity sourceBlockEntity =
                    faucet.getLevel().getBlockEntity(
                            sourceTankPos
                    );

            if (
                    sourceBlockEntity
                            instanceof FoundryTankBlockEntity sourceTank
            ) {
                renderedMetalId =
                        sourceTank.getStoredMetal();
            }
        }

        MoltenMetalDefinition metal =
                renderedMetalId != null
                        ? ModMoltenMetals.get(
                                renderedMetalId
                        ).orElse(null)
                        : null;

        if (metal == null) {
            return;
        }

        int cauldronDistance =
                cauldronTarget.distance();

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

        float cauldronBlockBaseY =
                -cauldronDistance;

        float cauldronBottomY =
                cauldronBlockBaseY
                        + CAULDRON_MIN_Y;

        float cauldronSurfaceY =
                cauldronBlockBaseY + Mth.lerp(
                        fillPercentage,
                        CAULDRON_MIN_Y,
                        CAULDRON_MAX_Y
                );

        float dripDisappearY =
                displayedMoltenAmount > 0.01f
                        ? Math.max(
                        cauldronBottomY,
                        cauldronSurfaceY
                                - MOLTEN_SURFACE_PENETRATION
                )
                        : cauldronBottomY;

        float streamBottomY;
        float streamInnerTopY;
        float streamOuterTopY;

        boolean renderBrokenDrips =
                false;

        float dripBreakupProgress =
                0.0f;

        float dripFieldBottomY =
                cauldronSurfaceY;

        float dripFieldTopY =
                cauldronSurfaceY;

        if (pouring) {
            streamBottomY =
                    Mth.lerp(
                            verticalProgress,
                            STREAM_OUTER_TOP_Y,
                            cauldronSurfaceY
                    );

            streamInnerTopY =
                    STREAM_INNER_TOP_Y;

            streamOuterTopY =
                    STREAM_OUTER_TOP_Y;
        } else {
            /*
             * Reconstruct the stream that existed when the Faucet stopped.
             *
             * If shutdown happened before the vertical stream appeared,
             * create only a short two-pixel drip from the remaining liquid
             * in the Faucet lip. That small drip still falls all the way to
             * the Cauldron instead of vanishing near the spout.
             */
            float initialStreamBottomY =
                    shutdownInitialVerticalProgress > 0.0001f
                            ? Mth.lerp(
                            shutdownInitialVerticalProgress,
                            STREAM_OUTER_TOP_Y,
                            cauldronSurfaceY
                    )
                            : STREAM_OUTER_TOP_Y
                            - MINIMUM_EARLY_DRIP_HEIGHT;

            if (verticalProgress < DRIP_BREAKUP_START) {
                /*
                 * Short continuous transition before the stream separates.
                 */
                streamBottomY =
                        Mth.lerp(
                                verticalProgress,
                                initialStreamBottomY,
                                cauldronSurfaceY
                        );

                streamInnerTopY =
                        Mth.lerp(
                                verticalProgress,
                                STREAM_INNER_TOP_Y,
                                streamBottomY
                        );

                streamOuterTopY =
                        Mth.lerp(
                                verticalProgress,
                                STREAM_OUTER_TOP_Y,
                                streamBottomY
                        );
            } else {
                renderBrokenDrips =
                        true;

                dripBreakupProgress = Mth.clamp(
                        (
                                verticalProgress
                                        - DRIP_BREAKUP_START
                        )
                                / (
                                1.0f
                                        - DRIP_BREAKUP_START
                        ),
                        0.0f,
                        1.0f
                );

                /*
                 * This defines where the pieces begin.
                 *
                 * Their eventual disappearance height is handled separately,
                 * so even a very short early-stop drip can travel the full
                 * remaining distance into the Cauldron.
                 */
                dripFieldBottomY =
                        Mth.lerp(
                                DRIP_BREAKUP_START,
                                initialStreamBottomY,
                                cauldronSurfaceY
                        );

                dripFieldTopY =
                        Mth.lerp(
                                DRIP_BREAKUP_START,
                                STREAM_OUTER_TOP_Y,
                                dripFieldBottomY
                        );

                streamBottomY =
                        cauldronSurfaceY;

                streamInnerTopY =
                        cauldronSurfaceY;

                streamOuterTopY =
                        cauldronSurfaceY;
            }
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
                                metal.animatedTexture()
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
                channelOuterZ,
                channelInnerZ,
                streamBottomY,
                streamInnerTopY,
                streamOuterTopY,
                renderBrokenDrips,
                dripStyle,
                dripBreakupProgress,
                dripFieldBottomY,
                dripFieldTopY,
                dripDisappearY,
                packedOverlay,
                frameMinV,
                frameMaxV,
                255
        );

        poseStack.popPose();
    }

    private float updateAndGetAnimationProgress(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        double currentRenderTime =
                faucet.getLevel().getGameTime()
                        + partialTick;

        float serverProgress = Mth.clamp(
                (float) faucet.getStreamAnimationStep()
                        / FoundryFaucetBlockEntity.STREAM_ANIMATION_STEPS,
                0.0f,
                1.0f
        );

        boolean pouringNow =
                faucet.isPouring();

        StreamRenderState renderState =
                streamRenderStates.computeIfAbsent(
                        faucet,
                        ignored -> StreamRenderState.createInitial(
                                serverProgress,
                                currentRenderTime,
                                pouringNow,
                                chooseDripStyle(faucet)
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

        /*
         * STARTUP -> SHUTDOWN
         *
         * Capture the exact amount that had visibly appeared. The following
         * drain animation is normalized from that amount, so even a very
         * quick click-off begins at drain progress zero.
         */
        if (
                renderState.wasPouring
                        && !pouringNow
        ) {
            renderState.drainStartProgress =
                    renderState.fillProgress;

            renderState.drainProgress =
                    0.0f;

            renderState.dripStyle =
                    chooseDripStyle(faucet);
        }

        /*
         * SHUTDOWN -> STARTUP
         *
         * Convert the undrained remainder back into startup progress. Rapid
         * repeated clicks therefore continue from the remaining amount
         * instead of restarting at an unrelated animation phase.
         */
        if (
                !renderState.wasPouring
                        && pouringNow
        ) {
            float remainingProgress =
                    renderState.drainStartProgress
                            * (
                            1.0f
                                    - renderState.drainProgress
                    );

            renderState.fillProgress =
                    Mth.clamp(
                            remainingProgress,
                            0.0f,
                            1.0f
                    );

            renderState.drainStartProgress =
                    0.0f;

            renderState.drainProgress =
                    0.0f;
        }

        renderState.wasPouring =
                pouringNow;

        float maximumChange =
                (float) elapsedTicks
                        / STREAM_ANIMATION_DURATION_TICKS;

        if (pouringNow) {
            renderState.fillProgress =
                    Math.min(
                            1.0f,
                            renderState.fillProgress
                                    + maximumChange
                    );

            return renderState.fillProgress;
        }

        if (renderState.drainStartProgress <= 0.0001f) {
            renderState.drainProgress =
                    1.0f;

            return 1.0f;
        }

        renderState.drainProgress =
                Math.min(
                        1.0f,
                        renderState.drainProgress
                                + maximumChange
                );

        return renderState.drainProgress;
    }

    /**
     * Produces an even, stable 50/50 choice using the Faucet position and
     * the game time at which the shutdown begins.
     *
     * No value is saved to the world because the style only affects the
     * short client-side drain animation.
     */
    private static DripStyle chooseDripStyle(
            FoundryFaucetBlockEntity faucet
    ) {
        long mixed =
                faucet.getBlockPos().asLong()
                        ^ faucet.getLevel().getGameTime()
                        * 31L;

        mixed ^=
                mixed >>> 33;

        mixed *=
                0xff51afd7ed558ccdL;

        mixed ^=
                mixed >>> 33;

        return (mixed & 1L) == 0L
                ? DripStyle.HORIZONTAL_ONLY
                : DripStyle.SPLIT_HALVES;
    }

    private static void renderMoltenPass(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float channelOuterZ,
            float channelInnerZ,
            float streamBottomY,
            float streamInnerTopY,
            float streamOuterTopY,
            boolean renderBrokenDrips,
            DripStyle dripStyle,
            float dripBreakupProgress,
            float dripFieldBottomY,
            float dripFieldTopY,
            float dripDisappearY,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        if (
                channelInnerZ
                        - channelOuterZ
                        > 0.0001f
        ) {
            renderChannelLiquid(
                    consumer,
                    pose,
                    channelOuterZ,
                    channelInnerZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }

        if (renderBrokenDrips) {
            renderBrokenDrips(
                    consumer,
                    pose,
                    dripFieldBottomY,
                    dripFieldTopY,
                    dripDisappearY,
                    dripBreakupProgress,
                    dripStyle,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        } else if (streamBottomY < streamOuterTopY) {
            renderStream(
                    consumer,
                    pose,
                    streamBottomY,
                    streamInnerTopY,
                    streamOuterTopY,
                    1.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    alpha
            );
        }
    }

    /**
     * Breaks the remaining stream into a loose 3 x 2 arrangement:
     *
     * - three separated pieces along the stream's height
     * - two vertical halves across the stream's width
     *
     * At breakupProgress 0 all pieces still meet, so the transition from
     * the continuous stream remains seamless.
     *
     * The right half falls just slightly more slowly than the left half,
     * preventing the pieces from looking perfectly synchronized.
     */
    private static void renderBrokenDrips(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float fieldBottomY,
            float fieldTopY,
            float disappearY,
            float breakupProgress,
            DripStyle dripStyle,
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

        float progress = Mth.clamp(
                breakupProgress,
                0.0f,
                1.0f
        );

        /*
         * Open the gaps between the three height sections quickly.
         */
        float horizontalGapProgress = Mth.clamp(
                progress
                        / 0.34f,
                0.0f,
                1.0f
        );

        horizontalGapProgress =
                smoothStep(
                        horizontalGapProgress
                );

        /*
         * Split the complete stream width into two halves.
         *
         * At zero:
         * - each half is exactly 50% wide
         * - centers are at -25% and +25%
         * - both halves touch perfectly in the middle
         *
         * A small center gap then opens.
         */
        float verticalSplitProgress = Mth.clamp(
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

        /*
         * Each shutdown randomly chooses one of two stable styles:
         *
         * HORIZONTAL_ONLY:
         * three full-width pieces, matching the first loose-drip design
         *
         * SPLIT_HALVES:
         * the same three height sections, each split into two vertical
         * halves with the right half falling slightly more slowly
         *
         * The choice is made only once when pouring stops, so it never
         * changes or flickers during one drain animation.
         */
        if (dripStyle == DripStyle.HORIZONTAL_ONLY) {
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

        /*
         * Bottom height section.
         */
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

        /*
         * Middle height section.
         */
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
        /*
         * Top height section.
         */
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

    /**
     * Renders one full-width detached piece for the horizontal-only style.
     */
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

    /**
     * Renders one horizontal height section as two vertical halves.
     *
     * The left half falls normally.
     * The right half falls a tiny bit more slowly.
     */
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
        /*
         * Left half: fractionally faster.
         */
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

        /*
         * Right half: fractionally slower.
         *
         * The difference is deliberately subtle so the breakup reads as
         * loose liquid rather than two completely unrelated streams.
         */
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

    /**
     * Renders one detached drip piece.
     *
     * normalizedXOffset is measured as a fraction of the original complete
     * stream width.
     */
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

        /*
         * Travel is based on the actual distance to the Cauldron target,
         * not on the original piece height.
         *
         * This is the important early-stop fix: a tiny drop formed near
         * the Faucet still falls the full distance to the Cauldron bottom
         * or molten surface.
         */
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

        /*
         * Clip the visible portion once it enters the bottom or molten
         * surface. The drop disappears only after its upper edge reaches
         * that target.
         */
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
            float channelOuterZ,
            float channelInnerZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int alpha
    ) {
        /*
         * Both edges are dynamic:
         *
         * startup  -> outer edge moves toward the lip
         * shutdown -> inner cutoff edge moves toward the lip
         */
        float channelDepthUv =
                channelInnerZ
                        - channelOuterZ;

        if (channelDepthUv <= 0.0001f) {
            return;
        }

        // =========================
        // TOP SURFACE
        // =========================

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

        /*
         * Outer face at the Faucet-lip side.
         */
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

        /*
         * Inner cutoff face.
         *
         * During shutdown this is the visible rear edge sweeping
         * outward as the remaining molten iron drains from the spout.
         */
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

        // West side
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

        // East side
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

        /*
         * minZ is the outer edge.
         * maxZ is the edge connected to the Faucet.
         */
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
                streamWidthUv,
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
                streamWidthUv,
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
                streamDepthUv,
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
                streamDepthUv,
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
                    streamWidthUv,
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

    private enum DripStyle {
        HORIZONTAL_ONLY,
        SPLIT_HALVES
    }

    private static final class StreamRenderState {

        private float fillProgress;
        private float drainStartProgress;
        private float drainProgress;

        private double lastRenderTime;
        private boolean wasPouring;
        private DripStyle dripStyle;

        private StreamRenderState(
                float fillProgress,
                float drainStartProgress,
                float drainProgress,
                double lastRenderTime,
                boolean wasPouring,
                DripStyle dripStyle
        ) {
            this.fillProgress =
                    fillProgress;

            this.drainStartProgress =
                    drainStartProgress;

            this.drainProgress =
                    drainProgress;

            this.lastRenderTime =
                    lastRenderTime;

            this.wasPouring =
                    wasPouring;

            this.dripStyle =
                    dripStyle;
        }

        private static StreamRenderState createInitial(
                float serverProgress,
                double currentRenderTime,
                boolean pouring,
                DripStyle dripStyle
        ) {
            if (pouring) {
                return new StreamRenderState(
                        serverProgress,
                        0.0f,
                        0.0f,
                        currentRenderTime,
                        true,
                        dripStyle
                );
            }

            if (serverProgress > 0.0001f) {
                /*
                 * A renderer created during an already-running shutdown can
                 * reconstruct the server's remaining animation.
                 */
                return new StreamRenderState(
                        0.0f,
                        1.0f,
                        1.0f - serverProgress,
                        currentRenderTime,
                        false,
                        dripStyle
                );
            }

            return new StreamRenderState(
                    0.0f,
                    0.0f,
                    1.0f,
                    currentRenderTime,
                    false,
                    dripStyle
            );
        }
    }

    /*
     * The rendered stream is part of the Faucet BlockEntityRenderer, but it
     * extends up to three blocks below the Faucet block.
     *
     * The default BER bounding box is only the Faucet's own one-block cube.
     * Expanding it downward keeps the renderer active whenever any visible
     * part of the stream is still inside the camera frustum.
     */
    @Override
    public AABB getRenderBoundingBox(
            FoundryFaucetBlockEntity faucet
    ) {
        BlockPos pos =
                faucet.getBlockPos();

        return new AABB(
                pos.getX(),
                pos.getY()
                        - FoundryFaucetBlockEntity.MAX_CAULDRON_DISTANCE,
                pos.getZ(),
                pos.getX() + 1.0,
                pos.getY() + 1.0,
                pos.getZ() + 1.0
        );
    }

    /*
     * Keep rendering while the expanded bounding box is partially outside the
     * screen. The bounding box above is still what defines the useful visible
     * scope of this renderer.
     */
    @Override
    public boolean shouldRenderOffScreen(
            FoundryFaucetBlockEntity faucet
    ) {
        return true;
    }
}
