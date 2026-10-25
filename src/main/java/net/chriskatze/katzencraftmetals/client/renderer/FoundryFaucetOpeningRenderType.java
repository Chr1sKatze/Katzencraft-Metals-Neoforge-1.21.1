package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;

/** Shared opening texture; hatch opacity and depth writes are independent of faucets. */
final class FoundryFaucetOpeningRenderType extends RenderType {
    static final int ORIGINAL_ALPHA = 191;
    static final int OPENING_ALPHA = 214;
    static final float ALPHA_MULTIPLIER = OPENING_ALPHA / (float) ORIGINAL_ALPHA;
    static final float HATCH_ALPHA = 0.50f;
    // Account for the texture's existing 191/255 alpha; vertex alpha is 1.
    static final float HATCH_ALPHA_MULTIPLIER = HATCH_ALPHA * 255.0f / ORIGINAL_ALPHA;
    private static final float[] SAVED_COLOR = new float[4];
    private static final RenderType BASE = entityTranslucent(FoundryTankRenderConstants.FAUCET_OVERLAY_TEXTURE);
    private static final RenderType INSTANCE = new FoundryFaucetOpeningRenderType(false);
    private static final RenderType HATCH = new FoundryFaucetOpeningRenderType(true);

    private FoundryFaucetOpeningRenderType(boolean hatch) {
        super(hatch ? "katzencraftmetals_hatch_opening" : "katzencraftmetals_faucet_opening", BASE.format(), BASE.mode(), BASE.bufferSize(),
                BASE.affectsCrumbling(), BASE.sortOnUpload(),
                () -> {
                    BASE.setupRenderState();
                    // Keep LEQUAL, texture, blend and lightmap; choose opacity per opening.
                    // The liquid behind the hatch must remain the depth-buffer surface.
                    if (hatch) COLOR_WRITE.setupRenderState();
                    float[] color = RenderSystem.getShaderColor();
                    System.arraycopy(color, 0, SAVED_COLOR, 0, 4);
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3] * (hatch ? HATCH_ALPHA_MULTIPLIER : ALPHA_MULTIPLIER));
                },
                () -> {
                    if (hatch) COLOR_WRITE.clearRenderState();
                    BASE.clearRenderState();
                    RenderSystem.setShaderColor(SAVED_COLOR[0], SAVED_COLOR[1], SAVED_COLOR[2], SAVED_COLOR[3]);
                });
    }

    static RenderType getHatch() {
        return HATCH;
    }

    static RenderType get() {
        return INSTANCE;
    }
}
