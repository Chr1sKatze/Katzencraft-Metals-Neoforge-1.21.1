package net.chriskatze.katzencraftmetals.client.renderer;

/**
 * Shared animation timing for every rendered molten-iron surface.
 *
 * This exactly mirrors the supplied vanilla still-lava metadata:
 *
 * frametime = 2
 *
 * frames:
 * 0, 1, 2, ... 18, 19, 18, ... 2, 1
 *
 * All Tanks, Casting Cauldrons, and Faucet streams therefore select
 * the same texture frame on the same game tick.
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

    public static Frame getFrame(
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

    public record Frame(
            int textureFrame,
            float minV,
            float maxV
    ) {
    }
}
