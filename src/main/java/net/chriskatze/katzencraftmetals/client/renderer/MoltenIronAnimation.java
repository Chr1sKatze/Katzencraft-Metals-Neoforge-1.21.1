package net.chriskatze.katzencraftmetals.client.renderer;

/**
 * Shared animation timing for rendered molten surfaces.
 *
 * This mirrors the supplied vanilla still-lava metadata:
 *
 * frametime = 2
 *
 * frames:
 * 0, 1, 2, ... 18, 19, 18, ... 2, 1
 *
 * Callers that need synchronized animation can use getFrame(gameTime).
 * Callers that should have a stable independent phase can additionally supply
 * a phase seed. Every render that uses the same seed remains perfectly
 * synchronized while different seeds begin at different points in the cycle.
 */
public final class MoltenIronAnimation {

    public static final int TEXTURE_FRAME_COUNT = 20;
    public static final int FRAME_TIME_TICKS = 2;

    /*
     * 0-19 contains 20 entries.
     * 18-1 contains another 18 entries.
     */
    private static final int SEQUENCE_LENGTH =
            TEXTURE_FRAME_COUNT * 2 - 2;

    private MoltenIronAnimation() {
    }

    /**
     * Original globally synchronized animation timing.
     */
    public static Frame getFrame(
            long gameTime
    ) {
        return getFrameFromTime(gameTime);
    }

    /**
     * Returns the molten animation frame with a deterministic phase offset.
     *
     * The phase seed is converted into a whole-frame offset, so animation speed
     * and frame timing remain completely unchanged. Renderers that pass the same
     * seed always stay synchronized with one another.
     */
    public static Frame getFrame(
            long gameTime,
            long phaseSeed
    ) {
        int phaseFrameOffset =
                (int) Math.floorMod(
                        mix64(phaseSeed),
                        SEQUENCE_LENGTH
                );

        long shiftedGameTime =
                gameTime
                        + (long) phaseFrameOffset
                        * FRAME_TIME_TICKS;

        return getFrameFromTime(shiftedGameTime);
    }

    private static Frame getFrameFromTime(
            long gameTime
    ) {
        int sequenceIndex =
                (int) Math.floorMod(
                        gameTime / FRAME_TIME_TICKS,
                        SEQUENCE_LENGTH
                );

        int textureFrame;

        if (sequenceIndex < TEXTURE_FRAME_COUNT) {
            textureFrame =
                    sequenceIndex;
        } else {
            textureFrame =
                    SEQUENCE_LENGTH
                            - sequenceIndex;
        }

        float minV =
                (float) textureFrame
                        / TEXTURE_FRAME_COUNT;

        float maxV =
                (float) (textureFrame + 1)
                        / TEXTURE_FRAME_COUNT;

        return new Frame(
                textureFrame,
                minV,
                maxV
        );
    }

    private static long mix64(
            long value
    ) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    public record Frame(
            int textureFrame,
            float minV,
            float maxV
    ) {
    }
}
