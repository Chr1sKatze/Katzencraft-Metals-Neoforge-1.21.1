package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/*
 * Shared visual fill state for the Casting Cauldron.
 *
 * Both the Cauldron renderer and the Faucet renderer use this same
 * displayed amount. This keeps the liquid surface and the bottom of
 * the falling stream at exactly the same visual height.
 */
final class CastingCauldronFillSmoother {

    /*
     * Match the visual rise speed to the Faucet's real transfer rate.
     *
     * Current values:
     *
     * 1 molten unit / 2 ticks = 0.5 molten units per tick.
     */
    private static final float FILL_UNITS_PER_TICK =
            (float) FoundryFaucetBlockEntity.TRANSFER_AMOUNT
                    / FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    private static final Map<CastingCauldronBlockEntity, FillRenderState>
            RENDER_STATES =
            new WeakHashMap<>();

    private CastingCauldronFillSmoother() {
    }

    static float getDisplayedMoltenAmount(
            CastingCauldronBlockEntity cauldron,
            float partialTick
    ) {
        if (cauldron.getLevel() == null) {
            return cauldron.getMoltenAmount();
        }

        double currentRenderTime =
                cauldron.getLevel().getGameTime()
                        + partialTick;

        FillRenderState renderState =
                RENDER_STATES.computeIfAbsent(
                        cauldron,
                        ignored -> new FillRenderState(
                                cauldron.getMoltenAmount(),
                                currentRenderTime
                        )
                );

        double elapsedTicks = Math.max(
                0.0,
                currentRenderTime
                        - renderState.lastRenderTime
        );

        renderState.lastRenderTime =
                currentRenderTime;

        float targetAmount =
                cauldron.getMoltenAmount();

        if (targetAmount > renderState.displayedAmount) {
            float maximumRise =
                    FILL_UNITS_PER_TICK
                            * (float) elapsedTicks;

            renderState.displayedAmount =
                    Math.min(
                            targetAmount,
                            renderState.displayedAmount
                                    + maximumRise
                    );
        } else if (targetAmount < renderState.displayedAmount) {
            /*
             * The Cauldron only decreases when its finished result is
             * removed. Snap down immediately in that case.
             */
            renderState.displayedAmount =
                    targetAmount;
        }

        return renderState.displayedAmount;
    }

    private static final class FillRenderState {

        private float displayedAmount;
        private double lastRenderTime;

        private FillRenderState(
                float displayedAmount,
                double lastRenderTime
        ) {
            this.displayedAmount =
                    displayedAmount;

            this.lastRenderTime =
                    lastRenderTime;
        }
    }
}
