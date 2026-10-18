package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.util.Mth;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetRenderConstants.*;

record FoundryFaucetStreamGeometry(
        float channelOuterZ,
        float channelInnerZ,
        float streamBottomY,
        float streamInnerTopY,
        float streamOuterTopY,
        boolean renderBrokenDrips,
        float dripBreakupProgress,
        float dripFieldBottomY,
        float dripFieldTopY
) {

    static FoundryFaucetStreamGeometry resolveDown(
            boolean pouring,
            float animationProgress,
            float shutdownStartProgress,
            float cauldronSurfaceY
    ) {
        return resolve(pouring, animationProgress, shutdownStartProgress, cauldronSurfaceY, true);
    }

    static FoundryFaucetStreamGeometry resolve(
            boolean pouring,
            float animationProgress,
            float shutdownStartProgress,
            float cauldronSurfaceY
    ) {
        return resolve(pouring, animationProgress, shutdownStartProgress, cauldronSurfaceY, false);
    }

    private static FoundryFaucetStreamGeometry resolve(
            boolean pouring,
            float animationProgress,
            float shutdownStartProgress,
            float cauldronSurfaceY,
            boolean bottomMounted
    ) {
        float startupProgress =
                pouring
                        ? animationProgress
                        : 0.0f;

        float shutdownProgress =
                pouring
                        ? 0.0f
                        : animationProgress;

        float channelOuterZ;
        float channelInnerZ;
        float verticalProgress;
        float shutdownInitialVerticalProgress =
                0.0f;

        if (pouring) {
            float horizontalFlowProgress =
                    Mth.clamp(
                            startupProgress
                                    / HORIZONTAL_PHASE_SHARE,
                            0.0f,
                            1.0f
                    );

            verticalProgress =
                    Mth.clamp(
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
            float initialHorizontalProgress =
                    Mth.clamp(
                            shutdownStartProgress
                                    / HORIZONTAL_PHASE_SHARE,
                            0.0f,
                            1.0f
                    );

            shutdownInitialVerticalProgress =
                    Mth.clamp(
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

            float horizontalDrainProgress =
                    Mth.clamp(
                            shutdownProgress
                                    / HORIZONTAL_PHASE_SHARE,
                            0.0f,
                            1.0f
                    );

            verticalProgress =
                    Mth.clamp(
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

            channelOuterZ =
                    Mth.lerp(
                            initialHorizontalProgress,
                            CHANNEL_MAX_Z,
                            CHANNEL_MIN_Z
                    );

            channelInnerZ =
                    Mth.lerp(
                            horizontalDrainProgress,
                            CHANNEL_MAX_Z,
                            channelOuterZ
                    );
        }

        if (bottomMounted) {
            channelOuterZ = CHANNEL_MIN_Z;
            channelInnerZ = CHANNEL_MIN_Z;
            // The bottom stream starts vertically immediately, without the side channel phase.
            if (pouring) {
                verticalProgress = Mth.clamp(startupProgress, 0.0f, 1.0f);
            } else {
                // Resume the existing nozzle-based drain at the visible length reached by
                // the connection-based stream. A stop inside the assembly uses its early-drip fallback.
                float initialBottomY = Mth.lerp(Mth.clamp(shutdownStartProgress, 0.0f, 1.0f),
                        DOWN_CONNECTION_Y, cauldronSurfaceY);
                shutdownInitialVerticalProgress = Mth.clamp(
                        (DOWN_NOZZLE_Y - initialBottomY) / (DOWN_NOZZLE_Y - cauldronSurfaceY),
                        0.0f, 1.0f);
            }
        }

        float bottomOriginY = pouring ? DOWN_CONNECTION_Y : DOWN_NOZZLE_Y;
        float innerTopY = bottomMounted ? bottomOriginY : STREAM_INNER_TOP_Y;
        float outerTopY = bottomMounted ? bottomOriginY : STREAM_OUTER_TOP_Y;

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
                            outerTopY,
                            cauldronSurfaceY
                    );

            streamInnerTopY =
                    innerTopY;

            streamOuterTopY =
                    outerTopY;
        } else {
            float initialStreamBottomY =
                    shutdownInitialVerticalProgress > 0.0001f
                            ? Mth.lerp(
                            shutdownInitialVerticalProgress,
                            outerTopY,
                            cauldronSurfaceY
                    )
                            : outerTopY
                            - MINIMUM_EARLY_DRIP_HEIGHT;

            if (verticalProgress < DRIP_BREAKUP_START) {
                streamBottomY =
                        Mth.lerp(
                                verticalProgress,
                                initialStreamBottomY,
                                cauldronSurfaceY
                        );

                streamInnerTopY =
                        Mth.lerp(
                                verticalProgress,
                                innerTopY,
                                streamBottomY
                        );

                streamOuterTopY =
                        Mth.lerp(
                                verticalProgress,
                                outerTopY,
                                streamBottomY
                        );
            } else {
                renderBrokenDrips =
                        true;

                dripBreakupProgress =
                        Mth.clamp(
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

                dripFieldBottomY =
                        Mth.lerp(
                                DRIP_BREAKUP_START,
                                initialStreamBottomY,
                                cauldronSurfaceY
                        );

                dripFieldTopY =
                        Mth.lerp(
                                DRIP_BREAKUP_START,
                                outerTopY,
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

        return new FoundryFaucetStreamGeometry(
                channelOuterZ,
                channelInnerZ,
                streamBottomY,
                streamInnerTopY,
                streamOuterTopY,
                renderBrokenDrips,
                dripBreakupProgress,
                dripFieldBottomY,
                dripFieldTopY
        );
    }
}
