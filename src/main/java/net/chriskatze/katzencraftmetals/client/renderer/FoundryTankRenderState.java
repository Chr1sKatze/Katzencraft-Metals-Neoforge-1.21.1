package net.chriskatze.katzencraftmetals.client.renderer;

final class FoundryTankRenderState {

    float displayedAmount;
    float lastTargetAmount;

    float transitionStartAmount;
    float transitionTargetAmount;

    double transitionStartTime;
    float transitionDuration;

    FoundryTankRenderState(
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
