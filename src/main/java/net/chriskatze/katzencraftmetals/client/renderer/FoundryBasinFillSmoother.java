package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/*
 * Shared visual fill state for the Foundry Basin.
 *
 * Both the Basin renderer and the Faucet renderer use this same
 * displayed amount. This keeps the liquid surface and the bottom of
 * the falling stream at exactly the same visual height.
 */
final class FoundryBasinFillSmoother {

    /*
     * Match the visual rise speed to the Faucet's real transfer rate.
     *
     * Current values:
     *
     * 6 molten units / 12 ticks = 0.5 molten units per tick.
     */
    private static final float FILL_UNITS_PER_TICK =
            (float) FoundryFaucetBlockEntity.TRANSFER_AMOUNT
                    / FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    private static final Map<FoundryBasinBlockEntity, FillRenderState>
            RENDER_STATES =
            new WeakHashMap<>();

    private FoundryBasinFillSmoother() {
    }

    static float getDisplayedMoltenAmount(
            FoundryBasinBlockEntity basin,
            float partialTick
    ) {
        if (basin.getLevel() == null) {
            return basin.getMoltenAmount();
        }

        double currentRenderTime =
                basin.getLevel().getGameTime()
                        + partialTick;

        FillRenderState renderState =
                RENDER_STATES.computeIfAbsent(
                        basin,
                        ignored -> new FillRenderState(
                                basin.getMoltenAmount(),
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
                basin.getMoltenAmount();

        if (renderState.displayedAmount <= 0.0f && targetAmount > 0.0f) {
            // A thin first layer is visible immediately without consuming the entire
            // confirmed batch; the normal rise can begin on this same frame.
            renderState.displayedAmount = Math.min(targetAmount, 0.001f);
        }

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
             * The Basin only decreases when its finished result is
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
