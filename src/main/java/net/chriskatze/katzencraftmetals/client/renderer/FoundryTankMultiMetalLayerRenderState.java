package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;

final class FoundryTankMultiMetalLayerRenderState {

    private static final float LIQUID_EPSILON =
            0.0001f;

    private static final float RISE_ANIMATION_TICKS =
            8.0f;

    private static final float DRAIN_ANIMATION_TICKS =
            FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    private Map<ResourceLocation, Float> displayedAmounts;
    private Map<ResourceLocation, Float> transitionStartAmounts;
    private Map<ResourceLocation, Float> transitionTargetAmounts;
    private Map<ResourceLocation, Float> lastTargetAmounts;

    private double transitionStartTime;
    private float transitionDuration;

    FoundryTankMultiMetalLayerRenderState(
            Map<ResourceLocation, Float> initialAmounts,
            double currentRenderTime
    ) {
        Map<ResourceLocation, Float> normalizedInitial =
                normalizeDisplayedMap(
                        initialAmounts
                );

        displayedAmounts =
                normalizedInitial;

        transitionStartAmounts =
                normalizedInitial;

        transitionTargetAmounts =
                normalizedInitial;

        lastTargetAmounts =
                normalizedInitial;

        transitionStartTime =
                currentRenderTime;

        transitionDuration =
                0.0f;
    }

    Map<ResourceLocation, Float> updateAndGet(
            Map<ResourceLocation, Float> requestedTargets,
            double currentRenderTime
    ) {
        Map<ResourceLocation, Float> normalizedTargets =
                normalizeDisplayedMap(
                        requestedTargets
                );

        updateDisplayedAmounts(
                currentRenderTime
        );

        if (!sameDisplayedMap(
                normalizedTargets,
                lastTargetAmounts
        )) {
            float oldTotal =
                    sumDisplayedAmounts(
                            displayedAmounts
                    );

            float newTotal =
                    sumDisplayedAmounts(
                            normalizedTargets
                    );

            transitionStartAmounts =
                    displayedAmounts;

            transitionTargetAmounts =
                    normalizedTargets;

            lastTargetAmounts =
                    normalizedTargets;

            transitionStartTime =
                    currentRenderTime;

            transitionDuration =
                    newTotal
                            >= oldTotal
                            ? RISE_ANIMATION_TICKS
                            : DRAIN_ANIMATION_TICKS;
        }

        updateDisplayedAmounts(
                currentRenderTime
        );

        return displayedAmounts;
    }

    private void updateDisplayedAmounts(
            double currentRenderTime
    ) {
        float progress =
                transitionDuration <= 0.0f
                        ? 1.0f
                        : Mth.clamp(
                        (float) (
                                (
                                        currentRenderTime
                                                - transitionStartTime
                                )
                                        / transitionDuration
                        ),
                        0.0f,
                        1.0f
                );

        Map<ResourceLocation, Float> updated =
                new LinkedHashMap<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            ResourceLocation metal =
                    definition.id();

            float start =
                    transitionStartAmounts.getOrDefault(
                            metal,
                            0.0f
                    );

            float target =
                    transitionTargetAmounts.getOrDefault(
                            metal,
                            0.0f
                    );

            float displayed =
                    Mth.lerp(
                            progress,
                            start,
                            target
                    );

            if (displayed > LIQUID_EPSILON) {
                updated.put(
                        metal,
                        displayed
                );
            }
        }

        displayedAmounts =
                Map.copyOf(
                        updated
                );
    }

    private static Map<ResourceLocation, Float> normalizeDisplayedMap(
            Map<ResourceLocation, Float> source
    ) {
        Map<ResourceLocation, Float> normalized =
                new LinkedHashMap<>();

        if (source != null) {
            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.heaviestFirst()
            ) {
                float amount =
                        Math.max(
                                0.0f,
                                source.getOrDefault(
                                        definition.id(),
                                        0.0f
                                )
                        );

                if (amount > LIQUID_EPSILON) {
                    normalized.put(
                            definition.id(),
                            amount
                    );
                }
            }
        }

        return Map.copyOf(
                normalized
        );
    }

    private static boolean sameDisplayedMap(
            Map<ResourceLocation, Float> first,
            Map<ResourceLocation, Float> second
    ) {
        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            float firstAmount =
                    first.getOrDefault(
                            definition.id(),
                            0.0f
                    );

            float secondAmount =
                    second.getOrDefault(
                            definition.id(),
                            0.0f
                    );

            if (
                    Math.abs(
                            firstAmount
                                    - secondAmount
                    ) > 0.00001f
            ) {
                return false;
            }
        }

        return true;
    }

    private static float sumDisplayedAmounts(
            Map<ResourceLocation, Float> amounts
    ) {
        float total =
                0.0f;

        if (amounts == null) {
            return total;
        }

        for (float amount : amounts.values()) {
            total +=
                    amount;
        }

        return total;
    }
}
