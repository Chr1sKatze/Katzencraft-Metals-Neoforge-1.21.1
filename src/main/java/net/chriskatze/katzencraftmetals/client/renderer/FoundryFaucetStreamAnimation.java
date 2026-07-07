package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.util.Mth;

import java.util.Map;

final class FoundryFaucetStreamAnimation {

    private FoundryFaucetStreamAnimation() {
    }

    static float updateAndGetAnimationProgress(
            FoundryFaucetBlockEntity faucet,
            float partialTick,
            Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState>
                    streamRenderStates
    ) {
        double currentRenderTime =
                faucet.getLevel().getGameTime()
                        + partialTick;

        float serverProgress =
                Mth.clamp(
                        (float) faucet.getStreamAnimationStep()
                                / FoundryFaucetBlockEntity.STREAM_ANIMATION_STEPS,
                        0.0f,
                        1.0f
                );

        boolean pouringNow =
                faucet.isPouring();

        FoundryFaucetStreamRenderState renderState =
                streamRenderStates.computeIfAbsent(
                        faucet,
                        ignored ->
                                FoundryFaucetStreamRenderState
                                        .createInitial(
                                                serverProgress,
                                                currentRenderTime,
                                                pouringNow,
                                                chooseDripStyle(
                                                        faucet
                                                )
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

        if (
                renderState.wasPouring
                        && !pouringNow
        ) {
            renderState.drainStartProgress =
                    renderState.fillProgress;

            renderState.drainProgress =
                    0.0f;

            renderState.dripStyle =
                    chooseDripStyle(
                            faucet
                    );
        }

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
                        / FoundryFaucetRenderConstants
                        .STREAM_ANIMATION_DURATION_TICKS;

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

    private static FoundryFaucetDripStyle chooseDripStyle(
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
                ? FoundryFaucetDripStyle.HORIZONTAL_ONLY
                : FoundryFaucetDripStyle.SPLIT_HALVES;
    }
}
