package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.util.Mth;

/** Authored Basin cavity: X/Z 2..14, floor Y=3, full liquid Y=15 (12 model pixels). */
final class FoundryBasinRenderGeometry {
    static final float FLOOR = 3.0f / 16.0f;
    static final float FULL_SURFACE = 15.0f / 16.0f;
    // Retain the established tiny wall clearance, including during the cooling crossfade.
    static final float WALL_MIN = 2.05f / 16.0f;
    static final float WALL_MAX = 13.95f / 16.0f;

    static float surface(float fraction) {
        return Mth.lerp(Mth.clamp(fraction, 0, 1), FLOOR, FULL_SURFACE);
    }
    private FoundryBasinRenderGeometry() {}
}
