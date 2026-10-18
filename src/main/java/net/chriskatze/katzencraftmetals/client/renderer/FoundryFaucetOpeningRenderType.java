package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;

/** Existing opening texture and entity-translucent state, with alpha-only modulation. */
final class FoundryFaucetOpeningRenderType extends RenderType {
    static final int ORIGINAL_ALPHA = 191;
    static final int OPENING_ALPHA = 214;
    static final float ALPHA_MULTIPLIER = OPENING_ALPHA / (float) ORIGINAL_ALPHA;
    private static final float[] SAVED_COLOR = new float[4];
    private static final RenderType BASE = entityTranslucent(FoundryTankRenderConstants.FAUCET_OVERLAY_TEXTURE);
    private static final RenderType INSTANCE = new FoundryFaucetOpeningRenderType();

    private FoundryFaucetOpeningRenderType() {
        super("katzencraftmetals_faucet_opening", BASE.format(), BASE.mode(), BASE.bufferSize(),
                BASE.affectsCrumbling(), BASE.sortOnUpload(),
                () -> {
                    BASE.setupRenderState();
                    float[] color = RenderSystem.getShaderColor();
                    System.arraycopy(color, 0, SAVED_COLOR, 0, 4);
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3] * ALPHA_MULTIPLIER);
                },
                () -> {
                    BASE.clearRenderState();
                    RenderSystem.setShaderColor(SAVED_COLOR[0], SAVED_COLOR[1], SAVED_COLOR[2], SAVED_COLOR[3]);
                });
    }

    static RenderType get() {
        return INSTANCE;
    }
}
