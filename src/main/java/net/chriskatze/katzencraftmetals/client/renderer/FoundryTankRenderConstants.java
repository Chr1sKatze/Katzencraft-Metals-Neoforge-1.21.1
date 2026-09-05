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

    static final ResourceLocation SURFACE_CONTACT_RIM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/foundry_surface_contact_rim.png"
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
            0.2f / 16.0f;

    static final float LIQUID_MAX_INSET =
            15.80f / 16.0f;

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
    static final float HOT_SPOT_MAX_SIZE = 16.0f * PIXEL;
    static final float HOT_SPOT_MARGIN = 5.25f * PIXEL;
    static final float HOT_SPOT_Y_OFFSET = 0.0030f;
    static final float HOT_SPOT_MAX_ALPHA = 0.90f;
    static final float HOT_SPOT_GLASS_FADE_DISTANCE = 1.25f * PIXEL;

    /*
     * Each potential bloom gets its own stable lifetime, so neighboring spots
     * do not breathe in sync. 30-60 ticks is roughly 1.5-3.0 seconds.
     */
    static final long HOT_SPOT_MIN_LIFETIME_TICKS = 30L;
    static final long HOT_SPOT_MAX_LIFETIME_TICKS = 100L;

    /*
     * Broad blooms are deliberately softer while small blooms can be hotter.
     * A little per-cycle random variation is applied on top of this range.
     */
    static final float HOT_SPOT_MIN_INTENSITY = 0.52f;
    static final float HOT_SPOT_MAX_INTENSITY = 1.00f;

    /*
     * Very soft bright contact at the molten-liquid / glass boundary.
     *
     * This must stay subtle; it should read like heat blooming at the edge,
     * not like a hard cartoon outline around the liquid.
     */
    static final float CONTACT_RIM_WIDTH_SMALL = 2.5f * PIXEL;
    static final float CONTACT_RIM_WIDTH_MEDIUM = 3.5f * PIXEL;
    static final float CONTACT_RIM_WIDTH_LARGE = 4.5f * PIXEL;
    static final int CONTACT_RIM_MEDIUM_SURFACE_CELLS = 4;
    static final int CONTACT_RIM_LARGE_SURFACE_CELLS = 16;
    static final float CONTACT_RIM_Y_OFFSET = 0.0022f;
    static final float CONTACT_RIM_MAX_ALPHA = 0.16f;
    static final float CONTACT_RIM_GLASS_FADE_DISTANCE = 1.75f * PIXEL;

    static final long SURFACE_PARTICLE_INTERVAL_TICKS = 8L;
    static final float SURFACE_PARTICLE_TOP_LIMIT = 0.94f;
    static final float SURFACE_SMOKE_HEADROOM_REQUIRED = 0.08f;
    static final float SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET = 0.02f;

    private FoundryTankRenderConstants() {
    }
}
