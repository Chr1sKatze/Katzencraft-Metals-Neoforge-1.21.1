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

    static FoundryFaucetStreamGeometry resolve(
            boolean pouring,
            float animationProgress,
            float shutdownStartProgress,
            float cauldronSurfaceY
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
