package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.util.Mth;

import java.util.Map;

public final class FoundryFaucetStreamAnimation {

    private static final Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState> VISUAL_STATES = new java.util.WeakHashMap<>();
    private static final Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState> SOUND_STATES = new java.util.WeakHashMap<>();

    static Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState> visualStates() { return VISUAL_STATES; }

    public static void clearSoundStates() { SOUND_STATES.clear(); }

    /** Follow the renderer's exact timeline without advancing/mutating its state.
     * Between frames (or offscreen), use the same animation function, not a sound delay.
     */
    public static boolean hasVisibleStream(FoundryFaucetBlockEntity faucet,
                                          net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity basin) {
        var visual = VISUAL_STATES.get(faucet);
        var sound = SOUND_STATES.get(faucet);
        if (visual == null && sound == null && !faucet.isPouring() && !faucet.isDraining()) return false;
        if (visual != null && (sound == null || visual.lastRenderTime >= sound.lastRenderTime)) {
            if (sound == null) {
                sound = FoundryFaucetStreamRenderState.createInitial(0, visual.lastRenderTime, false, visual.dripStyle);
                SOUND_STATES.put(faucet, sound);
            }
            sound.copyFrom(visual);
        }
        // A rendered fractional tick may be newer than this audio tick: never rewind it.
        float partial = sound == null ? 0 : (float) Math.max(0, sound.lastRenderTime - faucet.getLevel().getGameTime());
        float progress = updateAndGetAnimationProgress(faucet, partial, SOUND_STATES);
        return (faucet.isPouring() || progress < 0.9999f)
                && FoundryFaucetMetalResolver.resolve(faucet, basin) != null;
    }

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
