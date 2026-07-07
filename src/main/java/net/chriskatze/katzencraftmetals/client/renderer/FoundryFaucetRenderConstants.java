package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;

final class FoundryFaucetRenderConstants {

    static final float CHANNEL_MIN_X = 6.05f / 16.0f;
    static final float CHANNEL_MAX_X = 9.95f / 16.0f;

    static final float CHANNEL_MIN_Z = 10.0f / 16.0f;
    static final float CHANNEL_MAX_Z = 15.95f / 16.0f;

    static final float CHANNEL_BOTTOM_Y = 6.05f / 16.0f;
    static final float CHANNEL_TOP_Y = 7.05f / 16.0f;

    static final float STREAM_MIN_X = CHANNEL_MIN_X;
    static final float STREAM_MAX_X = CHANNEL_MAX_X;

    static final float STREAM_MAX_Z =
            CHANNEL_MIN_Z;

    static final float STREAM_MIN_Z =
            STREAM_MAX_Z - 2.0f / 16.0f;

    static final float STREAM_INNER_TOP_Y =
            CHANNEL_TOP_Y;

    static final float STREAM_OUTER_TOP_Y =
            CHANNEL_TOP_Y - 1.0f / 16.0f;

    static final float CHANNEL_WIDTH_UV =
            CHANNEL_MAX_X - CHANNEL_MIN_X;

    static final float STREAM_WIDTH_UV =
            STREAM_MAX_X - STREAM_MIN_X;

    static final float STREAM_DEPTH_UV =
            STREAM_MAX_Z - STREAM_MIN_Z;

    static final float CAULDRON_MIN_Y = 4.05f / 16.0f;
    static final float CAULDRON_MAX_Y = 14.25f / 16.0f;

    static final float STREAM_ANIMATION_DURATION_TICKS =
            FoundryFaucetBlockEntity.STREAM_ANIMATION_STEPS
                    * FoundryFaucetBlockEntity.STREAM_ANIMATION_INTERVAL;

    static final float HORIZONTAL_PHASE_SHARE =
            0.25f;

    static final float DRIP_BREAKUP_START =
            0.18f;

    static final float MINIMUM_EARLY_DRIP_HEIGHT =
            2.0f / 16.0f;

    static final float MOLTEN_SURFACE_PENETRATION =
            1.0f / 16.0f;

    private FoundryFaucetRenderConstants() {
    }
}
