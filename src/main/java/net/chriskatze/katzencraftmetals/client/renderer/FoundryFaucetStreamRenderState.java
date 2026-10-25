package net.chriskatze.katzencraftmetals.client.renderer;

final class FoundryFaucetStreamRenderState {

    float fillProgress;
    float drainStartProgress;
    float drainProgress;

    double lastRenderTime;
    boolean wasPouring;
    FoundryFaucetDripStyle dripStyle;

    void copyFrom(FoundryFaucetStreamRenderState source) {
        fillProgress = source.fillProgress;
        drainStartProgress = source.drainStartProgress;
        drainProgress = source.drainProgress;
        lastRenderTime = source.lastRenderTime;
        wasPouring = source.wasPouring;
        dripStyle = source.dripStyle;
    }

    private FoundryFaucetStreamRenderState(
            float fillProgress,
            float drainStartProgress,
            float drainProgress,
            double lastRenderTime,
            boolean wasPouring,
            FoundryFaucetDripStyle dripStyle
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

    static FoundryFaucetStreamRenderState createInitial(
            float serverProgress,
            double currentRenderTime,
            boolean pouring,
            FoundryFaucetDripStyle dripStyle
    ) {
        if (pouring) {
            return new FoundryFaucetStreamRenderState(
                    serverProgress,
                    0.0f,
                    0.0f,
                    currentRenderTime,
                    true,
                    dripStyle
            );
        }

        if (serverProgress > 0.0001f) {
            return new FoundryFaucetStreamRenderState(
                    0.0f,
                    1.0f,
                    1.0f - serverProgress,
                    currentRenderTime,
                    false,
                    dripStyle
            );
        }

        return new FoundryFaucetStreamRenderState(
                0.0f,
                0.0f,
                1.0f,
                currentRenderTime,
                false,
                dripStyle
        );
    }
}
