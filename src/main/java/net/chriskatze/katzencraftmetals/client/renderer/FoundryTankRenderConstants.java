package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared constants for the Foundry Tank block entity renderer split.
 */
final class FoundryTankRenderConstants {

    static final ResourceLocation SIDE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_side.png"
            );

    static final ResourceLocation TOP_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_top.png"
            );

    static final ResourceLocation FAUCET_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_tank_faucet_overlay.png"
            );

    static final ResourceLocation SURFACE_HOT_SPOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_surface_hotspot.png"
            );

    static final float PIXEL =
            1.0f / 16.0f;

    /*
     * The side texture only contains pixels in the first two and last two
     * columns, plus the top and bottom rows. Cropping these exact regions
     * lets neighboring Tank faces form one uninterrupted large frame.
     */
    static final float SIDE_VERTICAL_FRAME_WIDTH =
            PIXEL;

    static final float SIDE_HORIZONTAL_FRAME_HEIGHT =
            PIXEL;

    static final float TOP_PERIMETER_WIDTH =
            PIXEL;

    /*
     * The connected top/bottom frame can form a concave corner where two
     * orthogonal Tank neighbors exist but the diagonal Tank is missing.
     *
     * A one-pixel square must be rendered there or the two perimeter runs
     * leave a visible hole. These UVs sample one guaranteed opaque pixel from
     * the top row of foundry_tank_top.png.
     */
    static final float CORNER_CAP_U0 =
            7.0f * PIXEL;

    static final float CORNER_CAP_U1 =
            8.0f * PIXEL;

    static final float CORNER_CAP_V0 =
            0.0f;

    static final float CORNER_CAP_V1 =
            PIXEL;

    /*
     * Marker rows copied from the supplied 16x16 reference texture.
     *
     * Each normal marker is:
     *
     *   ##
     *   #
     *
     * The permanent one-pixel side rail is rendered separately.
     */
    static final int[] MARKER_ROWS = {
            3,
            7,
            11
    };

    static final float FAUCET_OVERLAY_OFFSET =
            0.002f;

    static final float LIQUID_INSET =
            0.12f / 16.0f;

    static final float LIQUID_MAX_INSET =
            15.88f / 16.0f;

    static final float LIQUID_EPSILON =
            0.0001f;

    static final float RISE_ANIMATION_TICKS =
            8.0f;

    static final float DRAIN_ANIMATION_TICKS =
            FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    /*
     * Molten-surface heat blooms.
     *
     * These are deliberately larger than the old 0.7-1.5 pixel re-rendered
     * liquid patches. A dedicated translucent texture makes them readable
     * through the Tank glass at any liquid height.
     */
    static final int HOT_SPOT_MIN_TARGET_COUNT = 2;
    static final int HOT_SPOT_MAX_TARGET_COUNT = 6;
    static final float HOT_SPOT_MIN_SIZE = 4.75f * PIXEL;
    static final float HOT_SPOT_MAX_SIZE = 10.0f * PIXEL;
    static final float HOT_SPOT_MARGIN = 5.25f * PIXEL;
    static final float HOT_SPOT_Y_OFFSET = 0.0030f;
    static final float HOT_SPOT_MAX_ALPHA = 0.82f;
    static final float HOT_SPOT_GLASS_FADE_DISTANCE = 1.25f * PIXEL;
    static final long HOT_SPOT_LIFETIME_TICKS = 44L;

    static final long SURFACE_PARTICLE_INTERVAL_TICKS = 8L;
    static final float SURFACE_PARTICLE_TOP_LIMIT = 0.94f;
    static final float SURFACE_SMOKE_HEADROOM_REQUIRED = 0.08f;
    static final float SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET = 0.02f;

    private FoundryTankRenderConstants() {
    }
}
